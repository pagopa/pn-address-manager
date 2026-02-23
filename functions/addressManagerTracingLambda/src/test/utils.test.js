const assert = require("assert");
const fs = require('fs');
const path = require('path');
const utils = require('../app/lib/utils');
const REQUEST_NORMALIZED_DATA = require('./normalizedRequestItem.json');
const RESPONSE_NORMALIZED_DATA = require('./normalizedResponseItem.json');

// Caricamento CSV
const REQUEST_CSV  = fs.readFileSync(path.join(__dirname, 'data', 'PN_ADDRESSES_RAW.csv'),  'utf-8');
const RESPONSE_CSV  = fs.readFileSync(path.join(__dirname, 'data', 'PN_ADDRESSES_NORMALIZED.csv'),  'utf-8');

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


// processNormalizerResponse

describe("processNormalizerResponse", () => {

  it("should return at least one record", async () => {
    const records = await utils.processNormalizerResponse(RESPONSE_NORMALIZED_DATA, RESPONSE_CSV);
    assert.ok(records.length > 0, "Nessun record prodotto");
  });

  it("should set service=NORMALIZER and type=RESPONSE on all records", async () => {
    const records = await utils.processNormalizerResponse(RESPONSE_NORMALIZED_DATA, RESPONSE_CSV);
    for (const r of records) {
      assert.strictEqual(r.service, 'NORMALIZER');
      assert.strictEqual(r.type,    'RESPONSE');
    }
  });

  describe("exact values from CSV first row", () => {
    let record;

    before(async () => {
      const records = await utils.processNormalizerResponse(RESPONSE_NORMALIZED_DATA, RESPONSE_CSV);
      record = records[0];
    });

    it("should find a record", () => {
      assert.ok(record, "Nessun record trovato");
    });

    // campi estratti dallo split di id col[0]
    it("correlationId",     () => assert.strictEqual(record.correlationId,    'VALIDATE_NORMALIZE_ADDRESSES_REQUEST.IUN_LUGA-ADJT-JTZP-202601-T-1'));
    it("responseCreatedAt", () => assert.strictEqual(record.responseCreatedAt, '2026-01-22T11:51:17.488420128'));
    it("addressIdx",        () => assert.strictEqual(record.addressIdx,        0));

    // metadati da data
    it("batchId",           () => assert.strictEqual(record.batchId,           RESPONSE_NORMALIZED_DATA.batchId));
    it("service",           () => assert.strictEqual(record.service,           'NORMALIZER'));
    it("type",              () => assert.strictEqual(record.type,              'RESPONSE'));

    // responseTimestamp è new Date().toISOString() → verifico solo che sia una stringa ISO valida
    it("responseTimestamp è una stringa ISO", () => {
      assert.strictEqual(typeof record.responseTimestamp, 'string');
      assert.ok(!isNaN(Date.parse(record.responseTimestamp)), "Non è una data ISO valida");
    });

    // campi dal CSV
    it("id                     col[0]",  () => assert.strictEqual(record.id,                     'VALIDATE_NORMALIZE_ADDRESSES_REQUEST.IUN_LUGA-ADJT-JTZP-202601-T-1#2026-01-22T11:51:17.488420128#0'));
    it("nRisultatoNorm          col[1]",  () => assert.strictEqual(record.nRisultatoNorm,          1));
    it("fPostalizzabile         col[2]",  () => assert.strictEqual(record.fPostalizzabile,         1));
    it("nErroreNorm             col[3]",  () => assert.strictEqual(record.nErroreNorm,             null));
    it("nErroreNormDescription  col[3]",  () => assert.strictEqual(record.nErroreNormDescription,  null));
    it("sSiglaProv              col[4]",  () => assert.strictEqual(record.sSiglaProv,              'CS'));
    it("sStatoUff               col[5]",  () => assert.strictEqual(record.sStatoUff,               null));
    it("sStatoAbb               col[6]",  () => assert.strictEqual(record.sStatoAbb,               null));
    it("sStatoSpedizione        col[7]",  () => assert.strictEqual(record.sStatoSpedizione,        'ITALIA'));
    it("sComuneUff              col[8]",  () => assert.strictEqual(record.sComuneUff,              null));
    it("sComuneAbb              col[9]",  () => assert.strictEqual(record.sComuneAbb,              null));
    it("sComuneSpedizione       col[10]", () => assert.strictEqual(record.sComuneSpedizione,       'COSENZA'));
    it("sFrazioneUff            col[11]", () => assert.strictEqual(record.sFrazioneUff,            null));
    it("sFrazioneAbb            col[12]", () => assert.strictEqual(record.sFrazioneAbb,            null));
    it("sFrazioneSpedizione     col[13]", () => assert.strictEqual(record.sFrazioneSpedizione,     'COSENZA'));
    it("sCivicoAltro            col[14]", () => assert.strictEqual(record.sCivicoAltro,            'SCALA B'));
    it("sCap                    col[15]", () => assert.strictEqual(record.sCap,                    '87100'));
    it("sPresso                 col[16]", () => assert.strictEqual(record.sPresso,                 null));
    it("sViaCompletaUff         col[17]", () => assert.strictEqual(record.sViaCompletaUff,         null));
    it("sViaCompletaAbb         col[18]", () => assert.strictEqual(record.sViaCompletaAbb,         null));
    it("sViaCompletaSpedizione  col[19]", () => assert.strictEqual(record.sViaCompletaSpedizione,  'VIA SENZA NOME'));
  });
});

