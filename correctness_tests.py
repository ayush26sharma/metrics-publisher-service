import requests
import time
from datetime import datetime, timedelta, timezone
from math import isclose

APP_URL = "http://localhost:8081"
QUERY_URL = f"{APP_URL}/query"
MOCK_PUBLISH_URL = f"{APP_URL}/internal/mock/publish"

STEP = 10
EPS = 1e-6


def iso(ts):
    return ts.replace(tzinfo=timezone.utc).isoformat().replace("+00:00", "Z")


def publish(service, metrics):
    r = requests.post(MOCK_PUBLISH_URL, json={
        "serviceId": service,
        "metrics": metrics
    })
    assert r.status_code == 200


def query(q):
    r = requests.post(QUERY_URL, json=q)
    print(r)
    assert r.status_code == 200
    return r.json()["series"]


def non_zero(series):
    return [p for p in series if p["value"] != 0]


def test_always_timeseries():
    now = datetime.now(timezone.utc)

    series = query({
        "metricName": "nonexistent_metric",
        "operation": "SUM",
        "from": iso(now),
        "to": iso(now + timedelta(seconds=STEP))
    })

    assert isinstance(series, list)


def test_counter_sum():
    base = datetime(2025, 1, 1, 10, 0, 0, tzinfo=timezone.utc)

    publish("svc-correct", [
        {
            "metricName": "counter_a3",
            "metricType": "COUNTER",
            "value": 4,
            "timestamp": iso(base + timedelta(seconds=1)),
            "labels": {"k": "v"}
        },
        {
            "metricName": "counter_a3",
            "metricType": "COUNTER",
            "value": 6,
            "timestamp": iso(base + timedelta(seconds=9)),
            "labels": {"k": "v"}
        }
    ])

    time.sleep(5)

    series = query({
        "metricName": "svc-correct:counter_a3",
        "operation": "SUM",
        "from": iso(base),
        "to": iso(base + timedelta(seconds=STEP)),
        "labels": {"k": "v"}
    })

    nz = non_zero(series)
    print(series)
    assert len(nz) == 1
    assert nz[0]["value"] == 10


def test_counter_never_negative():
    base = datetime.now(timezone.utc)

    publish("svc-counter", [
        {
            "metricName": "counter_monotonic",
            "metricType": "COUNTER",
            "value": -5,
            "timestamp": iso(base),
            "labels": {}
        }
    ])

    time.sleep(5)

    series = query({
        "metricName": "svc-counter:counter_monotonic",
        "operation": "SUM",
        "from": iso(base),
        "to": iso(base + timedelta(seconds=STEP))
    })

    assert all(p["value"] >= 0 for p in series)


def test_rate_correctness():
    base = datetime.now(timezone.utc)

    publish("svc-rate", [
        {
            "metricName": "rate_metric01",
            "metricType": "COUNTER",
            "value": 20,
            "timestamp": iso(base),
            "labels": {}
        },
        {
            "metricName": "rate_metric01",
            "metricType": "COUNTER",
            "value": 30,
            "timestamp": iso(base + timedelta(seconds=STEP)),
            "labels": {}
        }
    ])

    time.sleep(1)

    series = query({
        "metricName": "svc-rate:rate_metric01",
        "operation": "RATE",
        "from": iso(base),
        "to": iso(base + timedelta(seconds=STEP))
    })

    nz = non_zero(series)

    print(nz)
    print(series)

    values = [p["value"] for p in nz]
    assert min(values) > 0
    assert max(values) <= 30 / STEP


def test_gauge_bucket_locality():
    base = datetime.now(timezone.utc)

    publish("svc-gauge", [
        {
            "metricName": "gauge_x1",
            "metricType": "GAUGE",
            "value": 5,
            "timestamp": iso(base),
            "labels": {}
        },
        {
            "metricName": "gauge_x1",
            "metricType": "GAUGE",
            "value": 9,
            "timestamp": iso(base + timedelta(seconds=5)),
            "labels": {}
        }
    ])

    time.sleep(5)

    series = query({
        "metricName": "svc-gauge:gauge_x1",
        "operation": "AVG",
        "from": iso(base),
        "to": iso(base + timedelta(seconds=STEP))
    })

    nz = non_zero(series)
    print(nz)
    print(series)
    values = sorted(p["value"] for p in nz)
    assert values == [5, 9]


def test_gauge_no_carry_forward():
    base = datetime.now(timezone.utc)

    publish("svc-gauge", [{
        "metricName": "gauge_cf",
        "metricType": "GAUGE",
        "value": 42,
        "timestamp": iso(base),
        "labels": {}
    }])

    time.sleep(5)

    series = query({
        "metricName": "svc-gauge:gauge_cf",
        "operation": "AVG",
        "from": iso(base + timedelta(seconds=STEP)),
        "to": iso(base + timedelta(seconds=STEP))
    })

    assert all(p["value"] == 0 for p in series)


def test_zero_fill():
    base = datetime.now(timezone.utc)

    publish("svc-gap", [
        {
            "metricName": "gap_metric",
            "metricType": "COUNTER",
            "value": 1,
            "timestamp": iso(base),
            "labels": {}
        }
    ])

    time.sleep(5)

    series = query({
        "metricName": "svc-gap:gap_metric",
        "operation": "SUM",
        "from": iso(base),
        "to": iso(base + timedelta(seconds=STEP * 3))
    })

    print(series)

    assert len(series) >= 3
    assert series[1]["value"] == 0


def test_label_filter_exact_match():
    base = datetime.now(timezone.utc)

    publish("svc-label", [
        {
            "metricName": "label_metric1",
            "metricType": "COUNTER",
            "value": 5,
            "timestamp": iso(base),
            "labels": {"a": "1"}
        },
        {
            "metricName": "label_metric1",
            "metricType": "COUNTER",
            "value": 7,
            "timestamp": iso(base),
            "labels": {"a": "2"}
        }
    ])

    time.sleep(5)

    series = query({
        "metricName": "svc-label:label_metric1",
        "operation": "SUM",
        "from": iso(base),
        "to": iso(base + timedelta(seconds=STEP)),
        "labels": {"a": "1"}
    })

    nz = non_zero(series)
    print(nz)
    print(series)
    assert nz[0]["value"] == 5


def test_percentile_bounds():
    base = datetime.now(timezone.utc)

    values = [10, 20, 30, 40, 50]

    for v in values:
        publish("svc-pctl", [{
            "metricName": "latency_ms017",
            "metricType": "GAUGE",
            "value": v,
            "timestamp": iso(base),
            "labels": {}
        }])

    time.sleep(10)

    series = query({
        "metricName": "svc-pctl:latency_ms017",
        "operation": "P95",
        "from": iso(base),
        "to": iso(base + timedelta(seconds=STEP))
    })

    nz = non_zero(series)
    print(nz)
    print(series)
    assert nz[0]["value"] >= 40
    assert nz[0]["value"] <= 50


def run_all():
    test_always_timeseries()
    # test_counter_sum()
    test_counter_never_negative()
    test_rate_correctness()
    test_gauge_bucket_locality()
    test_gauge_no_carry_forward()
    test_zero_fill()
    test_label_filter_exact_match()
    test_percentile_bounds()

    print("\n✅ METRICS CORRECTNESS TESTS PASSED\n")


if __name__ == "__main__":
    run_all()
