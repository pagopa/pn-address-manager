echo "### CREATE QUEUES ###"
queues="local-address-inputs"
for qn in $(echo $queues | tr " " "\n"); do
  echo creating queue $qn ...
  aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
    sqs create-queue \
    --attributes '{"DelaySeconds":"2"}' \
    --queue-name $qn
done

echo "### CREATE EVENT BUS ###"
event_bus_name="my-event-bus"
aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
  events create-event-bus --name $event_bus_name

echo "### CREATE RULE WITH CUSTOM PATTERN ###"
rule_name="my-rule"
pattern='{"source": ["pn-address-manager"], "detail-type": ["AddressManagerOutcomeEvent"]}'
aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
  events put-rule --name $rule_name --event-pattern "$pattern" \
  --event-bus-name $event_bus_name

echo "### ENABLE RULE ###"
aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
  events enable-rule --name $rule_name \
  --event-bus-name $event_bus_name

echo "### ADD TARGET TO RULE ###"
target_arn="arn:aws:sqs:us-east-1:000000000000:local-address-inputs"
aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
  events put-targets --rule $rule_name \
  --targets "Id"="1","Arn"="$target_arn" \
  --event-bus-name $event_bus_name
