const { checkNormalizerItem } = require("../app/lib/utils");
const safeStorage = require("../app/lib/safeStorage");
const { handleEvent } = require("../app/eventHandler");
const originalDownload = safeStorage.downloadJson;
const assert = require("assert");
const { FirehoseClient, PutRecordBatchCommand } = require('@aws-sdk/client-firehose');
const { mockClient } = require("aws-sdk-client-mock");
const firehoseMock = mockClient(FirehoseClient);

describe("checkNormalizerItem", () => {
  const batchId = "test-batch-123";

  it("should return null during initialization phase (all nulls)", () => {
    const event = {
      normalizer: { batchId, oldFileKey: null, oldOutputFileKey: null, newFileKey: null, newOutputFileKey: null }
    };
    assert.strictEqual(checkNormalizerItem(event), null);
  });

  it("should return null when no changes are detected", () => {
    const event = {
      normalizer: {
        batchId,
        oldFileKey: "file.json", oldOutputFileKey: "out.json",
        newFileKey: "file.json", newOutputFileKey: "out.json"
      }
    };
    assert.strictEqual(checkNormalizerItem(event), null);
  });

  it("should return NORMALIZER_REQUEST when input file changes", () => {
    const event = {
      normalizer: {
        batchId,
        oldFileKey: "old.json", oldOutputFileKey: "out.json",
        newFileKey: "new.json", newOutputFileKey: "out.json"
      }
    };
    const result = checkNormalizerItem(event);
    assert.deepStrictEqual(result, { type: 'NORMALIZER_REQUEST', fileKey: 'new.json' });
  });

  it("should return NORMALIZER_RESPONSE when output file changes", () => {
    const event = {
      normalizer: {
        batchId,
        oldFileKey: "file.json", oldOutputFileKey: "old_out.json",
        newFileKey: "file.json", newOutputFileKey: "new_out.json"
      }
    };
    const result = checkNormalizerItem(event);
    assert.deepStrictEqual(result, { type: 'NORMALIZER_RESPONSE', fileKey: 'new_out.json' });
  });
});

describe("handleEvent - NORMALIZER Integration", () => {
  beforeEach(() => {
    firehoseMock.reset();
    safeStorage.downloadJson = async () => ({ mockData: "from_csv" });
  });

  afterEach(() => {
    safeStorage.downloadJson = originalDownload;
  });

  it("should skip processing if checkNormalizerItem returns null", async () => {
    const event = {
      eventType: "NORMALIZER",
      data: {
        normalizer: { batchId: "B1", oldFileKey: "same.json", newFileKey: "same.json", oldOutputFileKey: null, newOutputFileKey: null }
      }
    };
    const result = await handleEvent(event);
    assert.strictEqual(result.success, false);
    assert.ok(result.error.length > 0);
  });
});