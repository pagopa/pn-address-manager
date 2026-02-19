// Setup env vars BEFORE requiring any module
process.env.AWS_REGION = "eu-south-1";
process.env.DELIVERY_STREAM_NAME = "test-delivery-stream";
process.env.BATCH_SIZE = "500";

const { handleEvent } = require("../app/eventHandler.js");
const { FirehoseClient, PutRecordBatchCommand } = require('@aws-sdk/client-firehose');
const deduplicaRequestItem = require('./deduplicaRequestItem.json');
const deduplicaResponseItem = require('./deduplicaResponseItem.json');
const { buildDeduplicaRequestItem, buildDeduplicaResponseItem } = require("../app/lib/utils");
const { mockClient } = require("aws-sdk-client-mock");
const assert = require("assert");
const utils = require("../app/lib/utils.js");
const safeStorage = require("../app/lib/safeStorage");
const fs = require('fs');
const path = require('path');

const firehoseMock = mockClient(FirehoseClient);

const REQUEST_CSV  = fs.readFileSync(path.join(__dirname, 'data', 'PN_ADDRESSES_RAW.csv'),  'utf-8');

function buildSuccessResponse(count) {
  return {
    FailedPutCount: 0,
    RequestResponses: Array.from({ length: count }, () => ({ RecordId: "mock-record-id" })),
  };
}

// utils

describe("buildDeduplicaRequestItem", () => {
  it("builds a request item with correct fixed fields", () => {
    const result = buildDeduplicaRequestItem(deduplicaRequestItem.deduplicateRequest);

    assert.strictEqual(result.service, "DEDUPLICATE");
    assert.strictEqual(result.type, "REQUEST");
    assert.ok(result.requestTimestamp, "requestTimestamp should be set");
  });

  it("maps correlationId from masterIn.id", () => {
    const result = buildDeduplicaRequestItem(deduplicaRequestItem.deduplicateRequest);

    assert.strictEqual(result.correlationId, deduplicaRequestItem.deduplicateRequest.masterIn.id);
  });

  it("maps all masterIn fields correctly", () => {
    const result = buildDeduplicaRequestItem(deduplicaRequestItem.deduplicateRequest);
    const masterIn = deduplicaRequestItem.deduplicateRequest.masterIn;

    assert.strictEqual(result.master_in_id,                 masterIn.id);
    assert.strictEqual(result.master_in_provincia,          masterIn.provincia);
    assert.strictEqual(result.master_in_cap,                masterIn.cap);
    assert.strictEqual(result.master_in_localita,           masterIn.localita);
    assert.strictEqual(result.master_in_localitaAggiuntiva, masterIn.localitaAggiuntiva);
    assert.strictEqual(result.master_in_indirizzo,          masterIn.indirizzo);
    assert.strictEqual(result.master_in_indirizzoAggiuntivo,masterIn.indirizzoAggiuntivo);
    assert.strictEqual(result.master_in_stato,              masterIn.stato);
  });

  it("maps all slaveIn fields correctly", () => {
    const result = buildDeduplicaRequestItem(deduplicaRequestItem.deduplicateRequest);
    const slaveIn = deduplicaRequestItem.deduplicateRequest.slaveIn;

    assert.strictEqual(result.slave_in_id,                  slaveIn.id);
    assert.strictEqual(result.slave_in_provincia,           slaveIn.provincia);
    assert.strictEqual(result.slave_in_cap,                 slaveIn.cap);
    assert.strictEqual(result.slave_in_localita,            slaveIn.localita);
    assert.strictEqual(result.slave_in_localitaAggiuntiva,  slaveIn.localitaAggiuntiva);
    assert.strictEqual(result.slave_in_indirizzo,           slaveIn.indirizzo);
    assert.strictEqual(result.slave_in_indirizzoAggiuntivo, slaveIn.indirizzoAggiuntivo);
    assert.strictEqual(result.slave_in_stato,               slaveIn.stato);
  });
});

describe("buildDeduplicaResponseItem", () => {
  it("builds a response item with correct fixed fields", () => {
    const result = buildDeduplicaResponseItem(deduplicaResponseItem.deduplicateResponse);

    assert.strictEqual(result.service, "DEDUPLICATE");
    assert.strictEqual(result.type, "RESPONSE");
    assert.ok(result.responseTimestamp, "responseTimestamp should be set");
  });

  it("maps correlationId from masterOut.id", () => {
    const result = buildDeduplicaResponseItem(deduplicaResponseItem.deduplicateResponse);

    assert.strictEqual(result.correlationId, deduplicaResponseItem.deduplicateResponse.masterOut.id);
  });

  it("maps risultatoDedu and errore correctly", () => {
    const result = buildDeduplicaResponseItem(deduplicaResponseItem.deduplicateResponse);

    assert.strictEqual(result.risultatoDedu, deduplicaResponseItem.deduplicateResponse.risultatoDedu);
    assert.strictEqual(result.errore,        deduplicaResponseItem.deduplicateResponse.errore);
  });

  it("maps all masterOut fields correctly", () => {
    const result    = buildDeduplicaResponseItem(deduplicaResponseItem.deduplicateResponse);
    const masterOut = deduplicaResponseItem.deduplicateResponse.masterOut;

    assert.strictEqual(result.master_out_id,                      masterOut.id);
    assert.strictEqual(result.master_out_nRisultatoNorm,          masterOut.nRisultatoNorm);
    assert.strictEqual(result.master_out_nErroreNorm,             masterOut.nErroreNorm);
    assert.strictEqual(result.master_out_nErroreNormDescription,  masterOut.nErroreNormDescription);
    assert.strictEqual(result.master_out_sSiglaProv,              masterOut.sSiglaProv);
    assert.strictEqual(result.master_out_fPostalizzabile,         masterOut.fPostalizzabile);
    assert.strictEqual(result.master_out_sStatoSpedizione,        masterOut.sStatoSpedizione);
    assert.strictEqual(result.master_out_sComuneSpedizione,       masterOut.sComuneSpedizione);
    assert.strictEqual(result.master_out_sFrazioneSpedizione,     masterOut.sFrazioneSpedizione);
    assert.strictEqual(result.master_out_sCivicoAltro,            masterOut.sCivicoAltro);
    assert.strictEqual(result.master_out_sCap,                    masterOut.sCap);
    assert.strictEqual(result.master_out_sPresso,                 masterOut.sPresso);
    assert.strictEqual(result.master_out_sViaCompletaSpedizione,  masterOut.sViaCompletaSpedizione);
  });

  it("maps all slaveOut fields correctly", () => {
    const result   = buildDeduplicaResponseItem(deduplicaResponseItem.deduplicateResponse);
    const slaveOut = deduplicaResponseItem.deduplicateResponse.slaveOut;

    assert.strictEqual(result.slave_out_id,                       slaveOut.id);
    assert.strictEqual(result.slave_out_nRisultatoNorm,           slaveOut.nRisultatoNorm);
    assert.strictEqual(result.slave_out_nErroreNorm,              slaveOut.nErroreNorm);
    assert.strictEqual(result.slave_out_nErroreNormDescription,   slaveOut.nErroreNormDescription);
    assert.strictEqual(result.slave_out_sSiglaProv,               slaveOut.sSiglaProv);
    assert.strictEqual(result.slave_out_fPostalizzabile,          slaveOut.fPostalizzabile);
    assert.strictEqual(result.slave_out_sStatoSpedizione,         slaveOut.sStatoSpedizione);
    assert.strictEqual(result.slave_out_sComuneSpedizione,        slaveOut.sComuneSpedizione);
    assert.strictEqual(result.slave_out_sFrazioneSpedizione,      slaveOut.sFrazioneSpedizione);
    assert.strictEqual(result.slave_out_sCivicoAltro,             slaveOut.sCivicoAltro);
    assert.strictEqual(result.slave_out_sCap,                     slaveOut.sCap);
    assert.strictEqual(result.slave_out_sPresso,                  slaveOut.sPresso);
    assert.strictEqual(result.slave_out_sViaCompletaSpedizione,   slaveOut.sViaCompletaSpedizione);
  });
});

// handleEvent

describe("handleEvent", () => {
  beforeEach(() => {
    firehoseMock.reset();
  });

  it("returns error when event is null", async () => {
    const result = await handleEvent(null);

    assert.strictEqual(result.success, false);
    assert.strictEqual(result.error, "EventType is required");
    assert.strictEqual(firehoseMock.commandCalls(PutRecordBatchCommand).length, 0);
  });

  it("returns error when event has no eventType", async () => {
    const result = await handleEvent({ data: deduplicaRequestItem.deduplicateRequest });

    assert.strictEqual(result.success, false);
    assert.strictEqual(result.error, "EventType is required");
    assert.strictEqual(firehoseMock.commandCalls(PutRecordBatchCommand).length, 0);
  });

  it("returns error when event has no data", async () => {
    const result = await handleEvent({ eventType: "DEDUPLICATE_REQUEST" });

    assert.strictEqual(result.success, false);
    assert.strictEqual(result.error, "Missing data in event body");
    assert.strictEqual(firehoseMock.commandCalls(PutRecordBatchCommand).length, 0);
  });

  it("returns error for unknown eventType", async () => {
    const result = await handleEvent({
      eventType: "UNKNOWN_EVENT",
      data: deduplicaRequestItem.deduplicateRequest,
    });

    assert.strictEqual(result.success, false);
    assert.strictEqual(result.error, "Unknown eventType: UNKNOWN_EVENT");
    assert.strictEqual(firehoseMock.commandCalls(PutRecordBatchCommand).length, 0);
  });

  // DEDUPLICATE_REQUEST

  it("handles DEDUPLICATE_REQUEST and returns success", async () => {
    firehoseMock.on(PutRecordBatchCommand).resolves(buildSuccessResponse(1));

    const result = await handleEvent({
      eventType: deduplicaRequestItem.eventType,
      data: deduplicaRequestItem.deduplicateRequest,
    });

    assert.strictEqual(result.success, true);
    assert.strictEqual(firehoseMock.commandCalls(PutRecordBatchCommand).length, 1);
  });

  it("sends DEDUPLICATE_REQUEST to the correct DeliveryStream", async () => {
    firehoseMock.on(PutRecordBatchCommand).resolves(buildSuccessResponse(1));

    await handleEvent({
      eventType: deduplicaRequestItem.eventType,
      data: deduplicaRequestItem.deduplicateRequest,
    });

    const input = firehoseMock.commandCalls(PutRecordBatchCommand)[0].args[0].input;
    assert.strictEqual(input.DeliveryStreamName, "test-delivery-stream");
  });

  it("sends DEDUPLICATE_REQUEST with correctly built record payload", async () => {
    firehoseMock.on(PutRecordBatchCommand).resolves(buildSuccessResponse(1));

    await handleEvent({
      eventType: deduplicaRequestItem.eventType,
      data: deduplicaRequestItem.deduplicateRequest,
    });

    const input   = firehoseMock.commandCalls(PutRecordBatchCommand)[0].args[0].input;
    const decoded = input.Records[0].Data.toString("utf-8");
    const parsed  = JSON.parse(decoded.trim());

    const expected = buildDeduplicaRequestItem(deduplicaRequestItem.deduplicateRequest);
    assert.strictEqual(parsed.correlationId,  expected.correlationId);
    assert.strictEqual(parsed.service,        expected.service);
    assert.strictEqual(parsed.type,           expected.type);
    assert.strictEqual(parsed.master_in_id,   expected.master_in_id);
    assert.strictEqual(parsed.slave_in_id,    expected.slave_in_id);
  });

  // DEDUPLICATE_RESPONSE

  it("handles DEDUPLICATE_RESPONSE and returns success", async () => {
    firehoseMock.on(PutRecordBatchCommand).resolves(buildSuccessResponse(1));

    const result = await handleEvent({
      eventType: deduplicaResponseItem.eventType,
      data: deduplicaResponseItem.deduplicateResponse,
    });

    assert.strictEqual(result.success, true);
    assert.strictEqual(firehoseMock.commandCalls(PutRecordBatchCommand).length, 1);
  });

  it("sends DEDUPLICATE_RESPONSE to the correct DeliveryStream", async () => {
    firehoseMock.on(PutRecordBatchCommand).resolves(buildSuccessResponse(1));

    await handleEvent({
      eventType: deduplicaResponseItem.eventType,
      data: deduplicaResponseItem.deduplicateResponse,
    });

    const input = firehoseMock.commandCalls(PutRecordBatchCommand)[0].args[0].input;
    assert.strictEqual(input.DeliveryStreamName, "test-delivery-stream");
  });

  it("sends DEDUPLICATE_RESPONSE with correctly built record payload", async () => {
    firehoseMock.on(PutRecordBatchCommand).resolves(buildSuccessResponse(1));

    await handleEvent({
      eventType: deduplicaResponseItem.eventType,
      data: deduplicaResponseItem.deduplicateResponse,
    });

    const input   = firehoseMock.commandCalls(PutRecordBatchCommand)[0].args[0].input;
    const decoded = input.Records[0].Data.toString("utf-8");
    const parsed  = JSON.parse(decoded.trim());

    const expected = buildDeduplicaResponseItem(deduplicaResponseItem.deduplicateResponse);
    assert.strictEqual(parsed.correlationId,  expected.correlationId);
    assert.strictEqual(parsed.service,        expected.service);
    assert.strictEqual(parsed.type,           expected.type);
    assert.strictEqual(parsed.risultatoDedu,  expected.risultatoDedu);
    assert.strictEqual(parsed.master_out_id,  expected.master_out_id);
    assert.strictEqual(parsed.slave_out_id,   expected.slave_out_id);
  });

  // error propagation

  it("returns error when Firehose throws during DEDUPLICATE_REQUEST", async () => {
    firehoseMock.on(PutRecordBatchCommand).rejects(new Error("Firehose unavailable"));

    const result = await handleEvent({
      eventType: deduplicaRequestItem.eventType,
      data: deduplicaRequestItem.deduplicateRequest,
    });

    assert.strictEqual(result.success, false);
    assert.strictEqual(result.error, "Firehose unavailable");
  });

  it("returns error when Firehose throws during DEDUPLICATE_RESPONSE", async () => {
    firehoseMock.on(PutRecordBatchCommand).rejects(new Error("Firehose unavailable"));

    const result = await handleEvent({
      eventType: deduplicaResponseItem.eventType,
      data: deduplicaResponseItem.deduplicateResponse,
    });

    assert.strictEqual(result.success, false);
    assert.strictEqual(result.error, "Firehose unavailable");
  });
});

// NORMALIZER
describe("handleEvent - NORMALIZER Integration", () => {
  let originalCheckNormalizerItem;
  let originalDownloadText;

  beforeEach(() => {
    originalCheckNormalizerItem = utils.checkNormalizerItem;
    originalDownloadText = safeStorage.downloadText;
  });

  afterEach(() => {
    utils.checkNormalizerItem = originalCheckNormalizerItem;
    safeStorage.downloadText = originalDownloadText;
    firehoseMock.reset();
  });

  it("should skip processing if checkNormalizerItem returns null", async () => {
    utils.checkNormalizerItem = () => null;

    const event = { eventType: "NORMALIZER", data: { normalizer: { batchId: "B1" } } };
    const result = await handleEvent(event);

    assert.strictEqual(result.success, true);
    assert.strictEqual(result.message, "Normalizer skipped");
  });

  it("should process NORMALIZER_REQUEST if checkNormalizerItem returns type NORMALIZER_REQUEST", async () => {
    utils.checkNormalizerItem = () => ({ type: "NORMALIZER_REQUEST", fileKey: "file.json" });
    safeStorage.downloadText = async () => REQUEST_CSV;
    firehoseMock.on(PutRecordBatchCommand).resolves(buildSuccessResponse(1));

    const event = { eventType: "NORMALIZER", data: { normalizer: {} } };
    const result = await handleEvent(event);

    assert.strictEqual(result.success, true);
  });

  it("should process NORMALIZER_RESPONSE if checkNormalizerItem returns type NORMALIZER_RESPONSE", async () => {
    utils.checkNormalizerItem = () => ({ type: "NORMALIZER_RESPONSE", fileKey: "file.json" });
    safeStorage.downloadText = async () => REQUEST_CSV;
    firehoseMock.on(PutRecordBatchCommand).resolves(buildSuccessResponse(1));

    const event = { eventType: "NORMALIZER", data: { normalizer: {} } };
    const result = await handleEvent(event);

    assert.strictEqual(result.success, true);
  });

  it("should return error if downloadText throws", async () => {
    utils.checkNormalizerItem = () => ({ type: "NORMALIZER_REQUEST", fileKey: "file.json" });
    safeStorage.downloadText = async () => { throw new Error("Download failed"); };

    const event = { eventType: "NORMALIZER", data: { normalizer: {} } };
    const result = await handleEvent(event);

    assert.strictEqual(result.success, false);
    assert.strictEqual(result.error, "Download failed");
  });

  it("should return error for unknown NORMALIZER subtype", async () => {
    utils.checkNormalizerItem = () => ({ type: "UNKNOWN_TYPE", fileKey: "file.json" });
    safeStorage.downloadText = async () => REQUEST_CSV;

    const event = { eventType: "NORMALIZER", data: { normalizer: {} } };
    const result = await handleEvent(event);

    assert.strictEqual(result.success, false);
    assert.strictEqual(result.error, "Unknown eventType: UNKNOWN_TYPE");
  });

  it("should call PutRecordBatch with real NORMALIZER_REQUEST records parsed from CSV", async () => {
      // CSV reale con una riga
      const csvText =
          "VALIDATE_NORMALIZE_ADDRESSES_REQUEST.IUN_GPRZ-QKMW-KXPV-202403-W-1#2024-03-11T15:08:14.075913927#0;" +
          "CS;87100;Cosenza;Cosenza;via @FAIL-Irreperibile_AR 16;scala b;ITALIA\n";

      // Evento shaped come produzione
      const event = {
          eventType: "NORMALIZER",
          normalizer: {
              batchId:          "batch-001",
              oldFileKey:       null,
              oldOutputFileKey: null,
              newFileKey:       "s3://bucket/request.csv",
              newOutputFileKey: null
          }
      };

      // Mock safeStorage con CSV reale testuale
      safeStorage.downloadText = async () => csvText;
      firehoseMock.on(PutRecordBatchCommand).resolves(buildSuccessResponse(1));

      // checkNormalizerItem REALE (non mockato)
      const result = await handleEvent(event);

      // Verifica successo
      assert.strictEqual(result.success, true);

      // Verifica che Firehose sia stato effettivamente chiamato
      const calls = firehoseMock.commandCalls(PutRecordBatchCommand);
      assert.strictEqual(calls.length, 1, "PutRecordBatchCommand dovrebbe essere chiamato una volta");

      // Verifica il payload inviato a Firehose
      const input   = calls[0].args[0].input;
      assert.strictEqual(input.DeliveryStreamName, "test-delivery-stream");
      assert.strictEqual(input.Records.length, 1);

      const decoded = input.Records[0].Data.toString("utf-8");
      const parsed  = JSON.parse(decoded.trim());

      // Verifica campi estratti dallo split di idCodiceCliente
      assert.strictEqual(parsed.correlationId,    'VALIDATE_NORMALIZE_ADDRESSES_REQUEST.IUN_GPRZ-QKMW-KXPV-202403-W-1');
      assert.strictEqual(parsed.requestCreatedAt, '2024-03-11T15:08:14.075913927');
      assert.strictEqual(parsed.addressIdx,       0);
      assert.strictEqual(parsed.batchId,          'batch-001');
      assert.strictEqual(parsed.service,          'NORMALIZER');
      assert.strictEqual(parsed.type,             'REQUEST');

      // Verifica campi CSV
      assert.strictEqual(parsed.idCodiceCliente,  'VALIDATE_NORMALIZE_ADDRESSES_REQUEST.IUN_GPRZ-QKMW-KXPV-202403-W-1#2024-03-11T15:08:14.075913927#0');
      assert.strictEqual(parsed.provincia,        'CS');
      assert.strictEqual(parsed.cap,              '87100');
      assert.strictEqual(parsed.localita,         'Cosenza');
      assert.strictEqual(parsed.indirizzo,        'via @FAIL-Irreperibile_AR 16');
      assert.strictEqual(parsed.stato,            'ITALIA');
      assert.ok(!isNaN(Date.parse(parsed.requestTimestamp)), "requestTimestamp deve essere una data ISO valida");
  });
});