## Quando viene aggiornato questo file, aggiornare anche il commitId presente nel file initsh-for-testcontainer-sh

echo "### CREATE QUEUES ###"
queues="local-address-inputs local-address-inputs-DLQ"
for qn in $(echo $queues | tr " " "\n"); do
  echo creating queue $qn ...
  aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
    sqs create-queue \
    --attributes '{"DelaySeconds":"2"}' \
    --queue-name $qn
done

echo "### CREATE EVENT BUS ###"
event_bus_name="PN_ADDRESS_MANAGER_EVENTBUS_NAME"
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

echo "CREATE DYNAMODB TABLES"

echo "addressManager-AnagraficaClient"
aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name addressManager-AnagraficaClient \
    --attribute-definitions \
        AttributeName=cxId,AttributeType=S \
    --key-schema \
        AttributeName=cxId,KeyType=HASH \
    --time-to-live-specification \
        Enabled=true, AttributeName=ttl \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5

echo "addressManager-Cap"
aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name addressManager-Cap \
    --attribute-definitions \
        AttributeName=cap,AttributeType=S \
    --key-schema \
        AttributeName=cap,KeyType=HASH \
    --time-to-live-specification \
        Enabled=true, AttributeName=ttl \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5

echo "addressManager-Country"
aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name addressManager-Country \
    --attribute-definitions \
        AttributeName=country,AttributeType=S \
    --key-schema \
        AttributeName=country,KeyType=HASH \
    --time-to-live-specification \
        Enabled=true, AttributeName=ttl \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5

echo "addressManager-NormalizzatoreBatch"
aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name addressManager-NormalizzatoreBatch \
    --attribute-definitions \
        AttributeName=batchId,AttributeType=S \
        AttributeName=status,AttributeType=S \
        AttributeName=workingTtl,AttributeType=S \
    --key-schema \
        AttributeName=batchId,KeyType=HASH \
    --time-to-live-specification \
        Enabled=true, AttributeName=ttl \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5 \
    --global-secondary-indexes \
            "[
                {
                    \"IndexName\": \"status-index\",
                    \"KeySchema\": [{\"AttributeName\":\"status\",\"KeyType\":\"HASH\"}],
                    \"Projection\":{
                        \"ProjectionType\":\"ALL\"
                    }
                },
                {
                    \"IndexName\": \"status-workingTtl-index\",
                    \"KeySchema\": [{\"AttributeName\":\"status\",\"KeyType\":\"HASH\"},
                                    {\"AttributeName\":\"workingTtl\",\"KeyType\":\"RANGE\"}],
                    \"Projection\":{
                        \"ProjectionType\":\"ALL\"
                    }
                }
            ]"

echo "addressManager-PNRequest"
aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name addressManager-PNRequest \
    --attribute-definitions \
        AttributeName=correlationId,AttributeType=S \
        AttributeName=createdAt,AttributeType=S \
        AttributeName=batchId,AttributeType=S \
        AttributeName=status,AttributeType=S \
        AttributeName=lastReserved,AttributeType=S \
        AttributeName=sendStatus,AttributeType=S \
    --key-schema \
        AttributeName=correlationId,KeyType=HASH \
        AttributeName=createdAt,KeyType=RANGE \
    --time-to-live-specification \
        Enabled=true, AttributeName=ttl \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5 \
    --global-secondary-indexes \
            "[
                {
                    \"IndexName\": \"batchId-lastReserved-index\",
                    \"KeySchema\": [{\"AttributeName\":\"batchId\",\"KeyType\":\"HASH\"},
                                    {\"AttributeName\":\"lastReserved\",\"KeyType\":\"RANGE\"}],
                    \"Projection\":{
                        \"ProjectionType\":\"ALL\"
                    }
                },
                {
                    \"IndexName\": \"status-index\",
                    \"KeySchema\": [{\"AttributeName\":\"status\",\"KeyType\":\"HASH\"}],
                    \"Projection\":{
                        \"ProjectionType\":\"ALL\"
                    }
                },
                {
                    \"IndexName\": \"sendStatus-lastReserved-index\",
                    \"KeySchema\": [{\"AttributeName\":\"sendStatus\",\"KeyType\":\"HASH\"},
                                    {\"AttributeName\":\"lastReserved\",\"KeyType\":\"RANGE\"}],
                    \"Projection\":{
                        \"ProjectionType\":\"ALL\"
                    }
                }
            ]"

echo "addressManager-ShedLock"
aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name addressManager-ShedLock \
    --attribute-definitions \
        AttributeName=_id,AttributeType=S \
    --key-schema \
        AttributeName=_id,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5