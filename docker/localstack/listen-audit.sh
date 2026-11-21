#!/bin/sh
set -eu
cd "$(dirname "$0")/../.."

docker compose -f compose.local.yml exec -T localstack python3 -u - <<'PY'
import json
from datetime import datetime, timezone

import boto3
from botocore.config import Config

sqs = boto3.client("sqs", endpoint_url="http://localhost:4566",
                   region_name="us-east-1", aws_access_key_id="test",
                   aws_secret_access_key="test", config=Config(read_timeout=30))
url = sqs.get_queue_url(QueueName="workspace-audit-events")["QueueUrl"]
print(f"Listening: {url}\nCtrl+C to stop. Messages are NOT deleted (may repeat after 30s).", flush=True)
try:
    while True:
        response = sqs.receive_message(QueueUrl=url, MaxNumberOfMessages=10,
                                       WaitTimeSeconds=20, VisibilityTimeout=30)
        for message in response.get("Messages", []):
            print(f"\n[{datetime.now(timezone.utc).isoformat()}] {message['MessageId']}")
            try:
                print(json.dumps(json.loads(message["Body"]), indent=2, ensure_ascii=False))
            except ValueError:
                print(message["Body"])
except KeyboardInterrupt:
    print("\nStopped.")
PY
