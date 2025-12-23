from kafka import KafkaProducer
import json

producer = KafkaProducer(
    bootstrap_servers="127.0.0.1:9092",
    value_serializer=lambda v: json.dumps(v).encode("utf-8"),
)

message = {
  "serviceId": "service-A",
  "fetchedAt": "2025-01-01T10:00:05Z",
  "rawPayload": {
    "metrics": [
      {
        "metricName": "http_requests_total",
        "metricType": "counter",
        "value": 5,
        "labels": {
          "api": "/dummy",
          "method": "GET"
        },
        "timestamp": "2025-01-01T10:00:03Z"
      },
      {
        "metricName": "http_requests_total",
        "metricType": "counter",
        "value": 7,
        "labels": {
          "api": "/dummy",
          "method": "GET"
        },
        "timestamp": "2025-01-02T10:00:07Z"
      }
    ]
  }
}


producer.send("metrics-raw", message)
producer.flush()

print("Message sent successfully")
