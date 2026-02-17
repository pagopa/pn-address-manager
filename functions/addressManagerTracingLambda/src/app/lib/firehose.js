const { FirehoseClient, PutRecordBatchCommand } = require("@aws-sdk/client-firehose");

const firehoseClient = new FirehoseClient({
    region: process.env.AWS_REGION
});

const MAX_RETRIES = 3;

/**
 * Sending records batch to Firehose with failed items
 * @param {Array} itemsList
 * @param {Function} [sleepFn] - Optional sleep/delay function for retries
 */
async function putRecordBatch(itemsList, sleepFn) {
    try {
        if (!Array.isArray(itemsList) || itemsList.length === 0) {
            console.warn("Empty or invalid items list");
            return;
        }

        const records = itemsList.map(item => ({
            Data: Buffer.from(JSON.stringify(item) + '\n', 'utf-8')
        }));

        // Firehose batch size is 500 record for batch
        const BATCH_SIZE = Number(process.env.BATCH_SIZE) || 500;
        const results = [];
        const DELIVERY_STREAM_NAME = process.env.DELIVERY_STREAM_NAME;

        for (let i = 0; i < records.length; i += BATCH_SIZE) {
            const chunk = records.slice(i, i + BATCH_SIZE);
            const response = await sendBatchWithRetry(chunk, MAX_RETRIES, DELIVERY_STREAM_NAME, sleepFn);
            results.push(response);
        }

        return results;

    } catch (error) {
        console.error("Error in putRecordBatch:", error);
        throw error;
    }
}

/**
 * Sending batch with unprocessed items retries
 * @param {Array} records
 * @param {Number} retriesLeft
 * @param {String} deliveryStreamName
 * @param {Function} [sleepFn] - Optional sleep/delay function for retries
 */
async function sendBatchWithRetry(records, retriesLeft, deliveryStreamName, sleepFn = sleep) {
    const command = new PutRecordBatchCommand({
        DeliveryStreamName: deliveryStreamName,
        Records: records
    });

    const response = await firehoseClient.send(command);
    console.log(`PutRecordBatch - Failed: ${response.FailedPutCount}/${records.length}`);

    const failed = records.filter((_, i) => response.RequestResponses[i]?.ErrorCode);

    if (failed.length > 0 && retriesLeft > 0) {
        console.warn(`Retrying ${failed.length} failed records, ${retriesLeft} retries left`);
        await sleepFn(3000);
        return sendBatchWithRetry(failed, retriesLeft - 1, deliveryStreamName, sleepFn);
    }

    if (failed.length > 0) {
        console.error(`Failed to send ${failed.length} records after ${MAX_RETRIES} attempts`);
    }

    return response;
}

/**
 * Delay utility function
 */
function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}


module.exports = { putRecordBatch, sleep };
