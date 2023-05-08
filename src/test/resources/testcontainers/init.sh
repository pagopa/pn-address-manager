echo "### CREATE QUEUES FIFO ###"
queues_fifo="local-address-inputs.fifo"
for qn in $(echo $queues_fifo | tr " " "\n"); do
  echo creating queue fifo $qn ...
  aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
    sqs create-queue \
    --attributes '{"DelaySeconds":"2","FifoQueue": "true","ContentBasedDeduplication": "true"}' \
    --queue-name $qn
done

