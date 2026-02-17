const { putRecordBatch } = require('../app/lib/firehose.js');
const { FirehoseClient, PutRecordBatchCommand } = require("@aws-sdk/client-firehose");
const { mockClient } = require("aws-sdk-client-mock");
const assert = require("assert");

// Setup env vars before requiring the module
process.env.AWS_REGION = "eu-south-1";
process.env.DELIVERY_STREAM_NAME = "test-delivery-stream";
process.env.BATCH_SIZE = "500";
const MAX_RETRIES = 3;

const firehoseMock = mockClient(FirehoseClient);

/**
 * Builds a mock PutRecordBatch response where all records succeed.
 */
function buildSuccessResponse(count) {
  return {
    FailedPutCount: 0,
    RequestResponses: Array.from({ length: count }, () => ({
      RecordId: "mock-record-id",
    })),
  };
}

/**
 * Builds a mock PutRecordBatch response where specific indexes fail.
 * @param {number} count - total records in the batch
 * @param {number[]} failedIndexes - indexes of records that should fail
 */
function buildPartialFailureResponse(count, failedIndexes) {
  return {
    FailedPutCount: failedIndexes.length,
    RequestResponses: Array.from({ length: count }, (_, i) =>
      failedIndexes.includes(i)
        ? { ErrorCode: "ServiceUnavailableException", ErrorMessage: "Service unavailable" }
        : { RecordId: "mock-record-id" }
    ),
  };
}

describe("putRecordBatch", () => {
  beforeEach(() => {
    firehoseMock.reset();
  });

  it("returns early (undefined) when itemsList is an empty array", async () => {
    const result = await putRecordBatch([]);

    assert.strictEqual(result, undefined);
    assert.strictEqual(firehoseMock.commandCalls(PutRecordBatchCommand).length, 0);
  });

  it("returns early (undefined) when itemsList is not an array", async () => {
    const result = await putRecordBatch(null);

    assert.strictEqual(result, undefined);
    assert.strictEqual(firehoseMock.commandCalls(PutRecordBatchCommand).length, 0);
  });

  it("sends a single batch when items fit within BATCH_SIZE", async () => {
    const items = [{ id: 1 }, { id: 2 }, { id: 3 }];

    firehoseMock.on(PutRecordBatchCommand).resolves(buildSuccessResponse(items.length));

    const results = await putRecordBatch(items);

    assert.strictEqual(firehoseMock.commandCalls(PutRecordBatchCommand).length, 1);
    assert.strictEqual(results.length, 1);
  });

  it("sends multiple batches when items exceed BATCH_SIZE", async () => {
    process.env.BATCH_SIZE = "2";

    const items = [{ id: 1 }, { id: 2 }, { id: 3 }, { id: 4 }, { id: 5 }];

    firehoseMock.on(PutRecordBatchCommand).callsFake((input) =>
      Promise.resolve(buildSuccessResponse(input.Records.length))
    );

    const results = await putRecordBatch(items);

    assert.strictEqual(firehoseMock.commandCalls(PutRecordBatchCommand).length, 3);
    assert.strictEqual(results.length, 3);

    process.env.BATCH_SIZE = "500";
  });

  it("sends records with the correct DeliveryStreamName", async () => {
    const items = [{ id: 1 }];

    firehoseMock.on(PutRecordBatchCommand).resolves(buildSuccessResponse(1));

    await putRecordBatch(items);

    const calls = firehoseMock.commandCalls(PutRecordBatchCommand);
    assert.strictEqual(calls.length, 1);

    // aws-sdk-client-mock stores the raw SDK input under .args[0].input
    const sentInput = calls[0].args[0].input;
    assert.strictEqual(sentInput.DeliveryStreamName, "test-delivery-stream");
  });

  it("encodes each item as newline-terminated JSON in the Record Data buffer", async () => {
    const item = { id: 42, name: "test" };

    firehoseMock.on(PutRecordBatchCommand).resolves(buildSuccessResponse(1));

    await putRecordBatch([item]);

    const calls = firehoseMock.commandCalls(PutRecordBatchCommand);
    const sentInput = calls[0].args[0].input;
    const dataBuffer = sentInput.Records[0].Data;

    assert.ok(Buffer.isBuffer(dataBuffer), "Data should be a Buffer");
    assert.strictEqual(dataBuffer.toString("utf-8"), JSON.stringify(item) + "\n");
  });

  // --- Retry logic ---
  // The source calls sleep(3000) between retries which would exceed Mocha's
  // default 5 s timeout when MAX_RETRIES=3. We increase the timeout to 15 s
  // for retry tests.

  it("retries failed records and stops after a successful retry", async function () {
    this.timeout(15000);

    const items = [{ id: 1 }, { id: 2 }];

    // 1st call: record at index 0 fails  ->  1 record goes to retry
    // 2nd call: all succeed              ->  retry loop ends
    firehoseMock
      .on(PutRecordBatchCommand)
      .resolvesOnce(buildPartialFailureResponse(2, [0]))
      .resolvesOnce(buildSuccessResponse(1));

    await putRecordBatch(items);

    // 1 initial call + 1 successful retry = 2 total
    assert.strictEqual(firehoseMock.commandCalls(PutRecordBatchCommand).length, 2);
  });

  it("retries failed records up to MAX_RETRIES (3) times then gives up", async function () {
    this.timeout(15000);

    const items = [{ id: 1 }, { id: 2 }];

        // Always fail the last record → exhausts all 3 retries
        firehoseMock.on(PutRecordBatchCommand).callsFake((cmd) => {
          const count = cmd.Records.length;
          return Promise.resolve(buildPartialFailureResponse(count, [count - 1]));
        });

        await putRecordBatch(items);

        // 1 initial + 3 retries = 4 total calls
        assert.strictEqual(firehoseMock.commandCalls(PutRecordBatchCommand).length, 4);
  });

  // Error propagation

  it("throws and propagates errors from the Firehose client", async () => {
    const items = [{ id: 1 }];

    firehoseMock.on(PutRecordBatchCommand).rejects(new Error("Network failure"));

    await assert.rejects(
      () => putRecordBatch(items),
      (err) => {
        assert.strictEqual(err.message, "Network failure");
        return true;
      }
    );
  });
});
