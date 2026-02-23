// Setup env vars before requiring the module
process.env.AWS_REGION = "eu-south-1";
process.env.DELIVERY_STREAM_NAME = "test-delivery-stream";
process.env.BATCH_SIZE = "500";

const { putRecordBatch } = require('../app/lib/firehose.js');
const { FirehoseClient, PutRecordBatchCommand } = require("@aws-sdk/client-firehose");
const { mockClient } = require("aws-sdk-client-mock");
const assert = require("assert");

const firehoseMock = mockClient(FirehoseClient);

const noOpSleep = async () => {};

function buildSuccessResponse(count) {
  return {
    FailedPutCount: 0,
    RequestResponses: Array.from({ length: count }, () => ({
      RecordId: "mock-record-id",
    })),
  };
}

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
    process.env.BATCH_SIZE = "500";
  });

  it("returns early when itemsList is empty", async () => {
    const result = await putRecordBatch([], noOpSleep);

    assert.strictEqual(result, undefined);
    assert.strictEqual(firehoseMock.commandCalls(PutRecordBatchCommand).length, 0);
  });

  it("returns early when itemsList is not an array", async () => {
    const result = await putRecordBatch(null, noOpSleep);

    assert.strictEqual(result, undefined);
    assert.strictEqual(firehoseMock.commandCalls(PutRecordBatchCommand).length, 0);
  });

  it("sends a single batch when items fit within BATCH_SIZE", async () => {
    const items = [{ id: 1 }, { id: 2 }, { id: 3 }];

    firehoseMock.on(PutRecordBatchCommand)
      .resolves(buildSuccessResponse(items.length));

    const results = await putRecordBatch(items, noOpSleep);

    assert.strictEqual(firehoseMock.commandCalls(PutRecordBatchCommand).length, 1);
    assert.strictEqual(results.length, 1);
  });

  it("sends multiple batches when items exceed BATCH_SIZE", async () => {
    process.env.BATCH_SIZE = "2";

    const items = [{ id: 1 }, { id: 2 }, { id: 3 }, { id: 4 }, { id: 5 }];

    firehoseMock.on(PutRecordBatchCommand).callsFake((cmd) =>
      Promise.resolve(buildSuccessResponse(cmd.Records.length))
    );

    const results = await putRecordBatch(items, noOpSleep);

    assert.strictEqual(firehoseMock.commandCalls(PutRecordBatchCommand).length, 3);
    assert.strictEqual(results.length, 3);
  });

  it("sends records with correct DeliveryStreamName", async () => {
    const items = [{ id: 1 }];

    firehoseMock.on(PutRecordBatchCommand)
      .resolves(buildSuccessResponse(1));

    await putRecordBatch(items, noOpSleep);

    const call = firehoseMock.commandCalls(PutRecordBatchCommand)[0];
    const sentInput = call.args[0].input;

    assert.strictEqual(sentInput.DeliveryStreamName, "test-delivery-stream");
  });

  it("encodes items as newline-terminated JSON buffer", async () => {
    const item = { id: 42, name: "test" };

    firehoseMock.on(PutRecordBatchCommand)
      .resolves(buildSuccessResponse(1));

    await putRecordBatch([item], noOpSleep);

    const call = firehoseMock.commandCalls(PutRecordBatchCommand)[0];
    const buffer = call.args[0].input.Records[0].Data;

    assert.ok(Buffer.isBuffer(buffer));
    assert.strictEqual(buffer.toString("utf-8"), JSON.stringify(item) + "\n");
  });

  it("retries failed records and stops after successful retry", async () => {

    const items = [{ id: 1 }, { id: 2 }];

    firehoseMock
      .on(PutRecordBatchCommand)
      .resolvesOnce(buildPartialFailureResponse(2, [0])) // first call fails 1 record
      .resolvesOnce(buildSuccessResponse(1));            // retry succeeds

    await putRecordBatch(items, noOpSleep);

    // 1 initial + 1 retry
    assert.strictEqual(
      firehoseMock.commandCalls(PutRecordBatchCommand).length,
      2
    );
  });

    it("retries up to MAX_RETRIES (3) then throws error", async () => {

      const items = [{ id: 1 }, { id: 2 }];

      firehoseMock.on(PutRecordBatchCommand).callsFake((cmd) => {
        const count = cmd.Records.length;
        return Promise.resolve(buildPartialFailureResponse(count, [count - 1]));
      });

      await assert.rejects(
        () => putRecordBatch(items, noOpSleep),
        (err) => {
          assert.strictEqual(
            err.message,
            "Exceeded maxRetries while sending to Firehose"
          );

          assert.ok(Array.isArray(err.unprocessedRecords));
          assert.strictEqual(err.unprocessedRecords.length, 1);

          return true;
        }
      );

      assert.strictEqual(
        firehoseMock.commandCalls(PutRecordBatchCommand).length,
        4
      );
    });

  it("propagates Firehose client errors", async () => {

    firehoseMock.on(PutRecordBatchCommand)
      .rejects(new Error("Network failure"));

    await assert.rejects(
      () => putRecordBatch([{ id: 1 }], noOpSleep),
      (err) => {
        assert.strictEqual(err.message, "Network failure");
        return true;
      }
    );
  });
});
