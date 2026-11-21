#!/bin/sh
set -eu

if ! awslocal s3api head-bucket --bucket workspace-audit-local >/dev/null 2>&1; then
  awslocal s3api create-bucket --bucket workspace-audit-local --region us-east-1
fi

python3 - <<'PY'
import json

import boto3

options = dict(endpoint_url="http://localhost:4566", region_name="us-east-1",
               aws_access_key_id="test", aws_secret_access_key="test")
s3 = boto3.client("s3", **options)
sqs = boto3.client("sqs", **options)
bucket = "workspace-audit-local"
s3.put_bucket_versioning(Bucket=bucket, VersioningConfiguration={"Status": "Enabled"})
queue_url = sqs.create_queue(QueueName="workspace-audit-events")["QueueUrl"]
attributes = sqs.get_queue_attributes(QueueUrl=queue_url,
                                     AttributeNames=["QueueArn", "Policy"])["Attributes"]
queue_arn = attributes["QueueArn"]
policy = json.loads(attributes.get("Policy", '{"Version":"2012-10-17","Statement":[]}'))
statement_id = "AllowWorkspaceAuditBucket"
policy["Statement"] = [s for s in policy["Statement"] if s.get("Sid") != statement_id]
policy["Statement"].append({
    "Sid": statement_id,
    "Effect": "Allow",
    "Principal": {"Service": "s3.amazonaws.com"},
    "Action": "sqs:SendMessage",
    "Resource": queue_arn,
    "Condition": {
        "ArnEquals": {"aws:SourceArn": f"arn:aws:s3:::{bucket}"},
        "StringEquals": {"aws:SourceAccount": queue_arn.split(":")[4]},
    },
})
sqs.set_queue_attributes(QueueUrl=queue_url, Attributes={"Policy": json.dumps(policy)})
notification = s3.get_bucket_notification_configuration(Bucket=bucket)
notification.pop("ResponseMetadata", None)
notification_id = "workspace-audit-object-created"
queues = [q for q in notification.get("QueueConfigurations", [])
          if q.get("Id") != notification_id]
queues.append({"Id": notification_id, "QueueArn": queue_arn,
               "Events": ["s3:ObjectCreated:*"]})
notification["QueueConfigurations"] = queues
s3.put_bucket_notification_configuration(Bucket=bucket, NotificationConfiguration=notification)
print(f"Configured {bucket} -> {queue_arn}")
PY
