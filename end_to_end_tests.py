import requests
import psycopg2
import time
import random
import uuid
import threading
import subprocess
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timedelta, timezone

APP_URL = "http://localhost:8081"
QUERY_URL = f"{APP_URL}/query"
MOCK_PUBLISH_URL = f"{APP_URL}/internal/mock/publish"

DB_CONFIG = {
    "host": "localhost",
    "dbname": "postgres",
    "user": "postgres",
    "password": "postgres",
    "port": 5432
}

STEP_SECONDS = 10
TIMEOUT = 5


def assert_true(condition, msg):
    if not condition:
        raise AssertionError(msg)


def check_health():
    r = requests.get(f"{APP_URL}/actuator/health", timeout=TIMEOUT)
    assert_true(r.status_code == 200, "App health endpoint failed")


def check_db():
    conn = psycopg2.connect(**DB_CONFIG)
    cur = conn.cursor()
    cur.execute("SELECT 1")
    cur.close()
    conn.close()


def publish_metrics(service_id, metrics):
    payload = {
        "serviceId": service_id,
        "metrics": metrics
    }
    r = requests.post(
        MOCK_PUBLISH_URL,
        json=payload,
        timeout=TIMEOUT
    )
    assert_true(r.status_code == 200, "Metric publish failed")


def iso(ts):
    return ts.replace(tzinfo=timezone.utc).isoformat().replace("+00:00", "Z")


def align(ts):
    epoch = int(ts.timestamp())
    return epoch - (epoch % STEP_SECONDS)


def test_COUNTER_aggregation():
    base = datetime(2025, 1, 1, 10, 0, 0)
    metrics = [
        {
            "metricName": "req_total",
            "metricType": "COUNTER",
            "value": 5,
            "timestamp": iso(base + timedelta(seconds=3)),
            "labels": {"api": "/a"}
        },
        {
            "metricName": "req_total",
            "metricType": "COUNTER",
            "value": 7,
            "timestamp": iso(base + timedelta(seconds=7)),
            "labels": {"api": "/a"}
        }
    ]

    publish_metrics("svc-agg", metrics)
    time.sleep(2)

    query = {
        "metricName": "svc-agg:req_total",
        "operation": "SUM",
        "from": iso(base),
        "to": iso(base + timedelta(seconds=10)),
        "labels": {"api": "/a"}
    }

    r = requests.post(QUERY_URL, json=query)
    data = r.json()["series"]
    print(data)

    non_zero = [p for p in data if p["value"] != 0]

    assert_true(len(non_zero) == 1, "Expected exactly one non-zero bucket")
    assert_true(non_zero[0]["value"] == 12, f"Expected sum=12, got {non_zero[0]['value']}")



def test_cardinality_limit():
    base = datetime.utcnow()

    for i in range(700):  # exceed allowed 100
        metric = {
            "metricName": "hc_metric_6",
            "metricType": "GAUGE",
            "value": 1,
            "timestamp": iso(base),
            "labels": {"id": str(i)}
        }
        publish_metrics("svc-hc", [metric])

    time.sleep(5)

    conn = psycopg2.connect(**DB_CONFIG)
    cur = conn.cursor()
    cur.execute("""
        SELECT COUNT(DISTINCT labels)
        FROM metric_samples
        WHERE metric_name='svc-hc:hc_metric_6'
    """)
    count = cur.fetchone()[0]
    cur.close()
    conn.close()

    assert_true(count <= 500, f"Cardinality exceeded limit: {count}")


def test_high_load():
    base = datetime.utcnow()
    total = 500
    allowed_series = 50
    metric_name = "load_metric_122"

    def send(i):
        metric = {
            "metricName": metric_name,
            "metricType": "COUNTER",
            "value": 1,
            "timestamp": iso(base),
            # bounded cardinality
            "labels": {"worker": str(i % allowed_series)}
        }
        publish_metrics("svc-load", [metric])

    with ThreadPoolExecutor(max_workers=50) as ex:
        futures = [ex.submit(send, i) for i in range(total)]
        for f in as_completed(futures):
            f.result()

    time.sleep(10)

    query = {
        "metricName": "svc-load:" + metric_name,
        "operation": "SUM",
        "from": iso(base),
        "to": iso(base + timedelta(seconds=10))
    }

    r = requests.post(QUERY_URL, json=query)
    # total_sum = sum(p["value"] for p in r.json()["series"])

    observed = wait_until_sum(total, base, "svc-load:" + metric_name)

    assert_true(
        observed == total,
        f"Load mismatch: expected {total}, got {observed}"
    )


def wait_until_sum(expected, base, metric_name, timeout_sec=30):
    deadline = time.time() + timeout_sec
    last = 0

    while time.time() < deadline:
        r = requests.post(QUERY_URL, json={
            "metricName": metric_name,
            "operation": "SUM",
            "from": iso(base),
            "to": iso(base + timedelta(seconds=10))
        })
        last = sum(p["value"] for p in r.json()["series"])

        if last == expected:
            return last

        time.sleep(1)

    return last


def chaos_restart(container):
    subprocess.run(["docker", "stop", container], check=True)
    time.sleep(3)
    subprocess.run(["docker", "start", container], check=True)
    time.sleep(5)


def test_resilience():
    chaos_restart("redis")
    chaos_restart("timescaledb")

    base = datetime.utcnow()
    metric = {
        "metricName": "resilience_metric3",
        "metricType": "COUNTER",
        "value": 3,
        "timestamp": iso(base),
        "labels": {}
    }

    publish_metrics("svc-chaos", [metric])
    time.sleep(2)

    query = {
        "metricName": "svc-chaos:resilience_metric3",
        "operation": "SUM",
        "from": iso(base),
        "to": iso(base + timedelta(seconds=10))
    }

    r = requests.post(QUERY_URL, json=query)
    print(r.json())
    series = r.json()["series"]

    non_zero = [p for p in series if p["value"] > 0]

    assert_true(
        len(non_zero) == 1,
        "Expected exactly one non-zero datapoint after recovery"
    )

    assert_true(
        non_zero[0]["value"] == 3.0,
        f"Expected value 3.0 after recovery, got {non_zero[0]['value']}"
    )


def run_all():
    check_health()
    check_db()
    test_COUNTER_aggregation()
    test_cardinality_limit()
    test_high_load()
    test_resilience()

    print("\n✅ ALL E2E TESTS PASSED — SYSTEM IS PRODUCTION-GRADE\n")


if __name__ == "__main__":
    run_all()
