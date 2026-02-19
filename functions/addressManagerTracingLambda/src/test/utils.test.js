const assert = require("assert");
const fs = require('fs');
const path = require('path');
const utils = require('../app/lib/utils');
const REQUEST_NORMALIZED_DATA = require('./normalizedRequestItem.json');

// Caricamento CSV
const REQUEST_CSV  = fs.readFileSync(path.join(__dirname, 'data', 'PN_ADDRESSES_RAW.csv'),  'utf-8');

// checkNormalizerItem

describe("checkNormalizerItem", () => {
  const batchId = "test-batch-123";

  it("should return null during initialization phase (all nulls)", () => {
    const event = {
      normalizer: { batchId, oldFileKey: null, oldOutputFileKey: null, newFileKey: null, newOutputFileKey: null }
    };
    assert.strictEqual(utils.checkNormalizerItem(event), null);
  });

  it("should return null when no changes are detected", () => {
    const event = {
      normalizer: {
        batchId,
        oldFileKey: "file.json", oldOutputFileKey: "out.json",
        newFileKey: "file.json", newOutputFileKey: "out.json"
      }
    };
    assert.strictEqual(utils.checkNormalizerItem(event), null);
  });

  it("should return NORMALIZER_REQUEST when input file changes", () => {
    const event = {
      normalizer: {
        batchId,
        oldFileKey: "old.json", oldOutputFileKey: "out.json",
        newFileKey: "new.json", newOutputFileKey: "out.json"
      }
    };
    const result = utils.checkNormalizerItem(event);
    assert.deepStrictEqual(result, { type: 'NORMALIZER_REQUEST', fileKey: 'new.json' });
  });
});


// processNormalizerRequest

describe("processNormalizerRequest", () => {

  it("should return at least one record", async () => {
    const records = await utils.processNormalizerRequest(REQUEST_NORMALIZED_DATA, REQUEST_CSV);
    assert.ok(records.length > 0, "Nessun record prodotto");
  });

  it("should set service=NORMALIZER and type=REQUEST on all records", async () => {
    const records = await utils.processNormalizerRequest(REQUEST_NORMALIZED_DATA, REQUEST_CSV);
    for (const r of records) {
      assert.strictEqual(r.service, 'NORMALIZER');
      assert.strictEqual(r.type,    'REQUEST');
    }
  });

  describe("exact values from CSV first row", () => {
    let record;

    before(async () => {
      const records = await utils.processNormalizerRequest(REQUEST_NORMALIZED_DATA, REQUEST_CSV);
      record = records[0];
    });

    it("should find a record", () => {
      assert.ok(record, "Nessun record trovato");
    });

    // campi estratti dallo split di idCodiceCliente col[0]
    it("correlationId",    () => assert.strictEqual(record.correlationId,    'VALIDATE_NORMALIZE_ADDRESSES_REQUEST.IUN_GPRZ-QKMW-KXPV-202403-W-1'));
    it("requestCreatedAt", () => assert.strictEqual(record.requestCreatedAt, '2024-03-11T15:08:14.075913927'));
    it("addressIdx",       () => assert.strictEqual(record.addressIdx,       0));

    // metadati da data
    it("batchId",          () => assert.strictEqual(record.batchId,          REQUEST_NORMALIZED_DATA.batchId));
    it("service",          () => assert.strictEqual(record.service,          'NORMALIZER'));
    it("type",             () => assert.strictEqual(record.type,             'REQUEST'));

    // requestTimestamp è new Date().toISOString() → verifico solo che sia una stringa ISO valida
    it("requestTimestamp è una stringa ISO", () => {
      assert.strictEqual(typeof record.requestTimestamp, 'string');
      assert.ok(!isNaN(Date.parse(record.requestTimestamp)), "Non è una data ISO valida");
    });

    // campi dal CSV
    it("idCodiceCliente        col[0]", () => assert.strictEqual(record.idCodiceCliente,     'VALIDATE_NORMALIZE_ADDRESSES_REQUEST.IUN_GPRZ-QKMW-KXPV-202403-W-1#2024-03-11T15:08:14.075913927#0'));
    it("provincia              col[1]", () => assert.strictEqual(record.provincia,            'CS'));
    it("cap                    col[2]", () => assert.strictEqual(record.cap,                  '87100'));
    it("localita               col[3]", () => assert.strictEqual(record.localita,             'Cosenza'));
    it("localitaAggiuntiva     col[4]", () => assert.strictEqual(record.localitaAggiuntiva,   'Cosenza'));
    it("indirizzo              col[5]", () => assert.strictEqual(record.indirizzo,            'via @FAIL-Irreperibile_AR 16'));
    it("indirizzoAggiuntivo    col[6]", () => assert.strictEqual(record.indirizzoAggiuntivo,  'scala b'));
    it("stato                  col[7]", () => assert.strictEqual(record.stato,                'ITALIA'));
  });
});


