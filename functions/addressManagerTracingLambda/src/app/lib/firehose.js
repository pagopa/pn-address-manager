const { FirehoseClient, PutRecordBatchCommand } = require("@aws-sdk/client-firehose");
const firehoseClient = new FirehoseClient({
    region: process.env.AWS_REGION
});
const MAX_RETRIES = 3;
const DEFAULT_BATCH_SIZE = 500;

async function putRecordBatch(itemsList) {

    if (!Array.isArray(itemsList) || itemsList.length === 0) {
        console.warn("Empty or invalid items list");
        return;
    }

    const batchSize = Number(process.env.BATCH_SIZE) || DEFAULT_BATCH_SIZE;
    const streamName = process.env.DELIVERY_STREAM_NAME;
    const records = itemsList.map(item => ({
        Data: Buffer.from(JSON.stringify(item) + '\n')
    }));
    const results = [];

    for (let i = 0; i < records.length; i += batchSize) {
        const chunk = records.slice(i, i + batchSize);
        const resp = await putBatchWithRetry(chunk, streamName, MAX_RETRIES);
        results.push(resp);
    }

    return results;
}

async function putBatchWithRetry(records, streamName, maxRetries = 3) {

    let unprocessed = records;
    let attempt = 0;
    let lastResponse;

    while (unprocessed && unprocessed.length > 0) {

        const resp = await firehoseClient.send(
            new PutRecordBatchCommand({
                DeliveryStreamName: streamName,
                Records: unprocessed
            })
        );

        lastResponse = resp;
        unprocessed = extractUnprocessed(resp, unprocessed);
        if (!unprocessed || unprocessed.length === 0) {
            break;
        }

        attempt++;
        if (attempt >= maxRetries) {
            const err = new Error("Exceeded maxRetries while sending to Firehose");
            err.unprocessedRecords = unprocessed;
            throw err;
        }

        const delay = Math.floor(Math.pow(2, attempt) * 100 + Math.random() * 100);
        await new Promise(r => setTimeout(r, delay));
    }

    return lastResponse;
}

function extractUnprocessed(response, sentRecords) {
    const responses = response?.RequestResponses;
    if (!Array.isArray(responses) || responses.length < sentRecords.length) {
        return response?.FailedPutCount > 0 ? sentRecords : [];
    }
    return sentRecords.filter((_, i) => responses[i]?.ErrorCode);
}

module.exports = { putRecordBatch };