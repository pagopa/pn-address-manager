const { parseCsv } = require('./csvUtils');
const { postelNErrorNormFromCode } = require('./postelNErrorNorm');

const DEDUPLICATE_SERVICE = 'DEDUPLICATE';
const NORMALIZER_SERVICE = 'NORMALIZER';
const REQUEST = 'REQUEST';
const RESPONSE = 'RESPONSE';

function buildDeduplicaRequestItem(req) {

    return {
        correlationId: req.masterIn.id,
        service: DEDUPLICATE_SERVICE,
        type: REQUEST,
        requestTimestamp: new Date().toISOString(),

        // SLAVE IN
        slave_in_id: req.slaveIn.id,
        slave_in_provincia: req.slaveIn.provincia,
        slave_in_cap: req.slaveIn.cap,
        slave_in_localita: req.slaveIn.localita,
        slave_in_localitaAggiuntiva: req.slaveIn.localitaAggiuntiva,
        slave_in_indirizzo: req.slaveIn.indirizzo,
        slave_in_indirizzoAggiuntivo: req.slaveIn.indirizzoAggiuntivo,
        slave_in_stato: req.slaveIn.stato,

        // MASTER IN
        master_in_id: req.masterIn.id,
        master_in_provincia: req.masterIn.provincia,
        master_in_cap: req.masterIn.cap,
        master_in_localita: req.masterIn.localita,
        master_in_localitaAggiuntiva: req.masterIn.localitaAggiuntiva,
        master_in_indirizzo: req.masterIn.indirizzo,
        master_in_indirizzoAggiuntivo: req.masterIn.indirizzoAggiuntivo,
        master_in_stato: req.masterIn.stato
    };
}

function buildDeduplicaResponseItem(res) {

    return {
        correlationId: res.masterOut.id,
        service: DEDUPLICATE_SERVICE,
        type: RESPONSE,
        responseTimestamp: new Date().toISOString(),

        risultatoDedu: res.risultatoDedu,
        errore: res.errore,

        // SLAVE OUT
        slave_out_id: res.slaveOut.id,
        slave_out_nRisultatoNorm: res.slaveOut.nRisultatoNorm,
        slave_out_nErroreNorm: res.slaveOut.nErroreNorm,
        slave_out_nErroreNormDescription: res.slaveOut.nErroreNormDescription,
        slave_out_sSiglaProv: res.slaveOut.sSiglaProv,
        slave_out_fPostalizzabile: res.slaveOut.fPostalizzabile,
        slave_out_sStatoSpedizione: res.slaveOut.sStatoSpedizione,
        slave_out_sComuneSpedizione: res.slaveOut.sComuneSpedizione,
        slave_out_sFrazioneSpedizione: res.slaveOut.sFrazioneSpedizione,
        slave_out_sCivicoAltro: res.slaveOut.sCivicoAltro,
        slave_out_sCap: res.slaveOut.sCap,
        slave_out_sPresso: res.slaveOut.sPresso,
        slave_out_sViaCompletaSpedizione: res.slaveOut.sViaCompletaSpedizione,

        // MASTER OUT
        master_out_id: res.masterOut.id,
        master_out_nRisultatoNorm: res.masterOut.nRisultatoNorm,
        master_out_nErroreNorm: res.masterOut.nErroreNorm,
        master_out_nErroreNormDescription: res.masterOut.nErroreNormDescription,
        master_out_sSiglaProv: res.masterOut.sSiglaProv,
        master_out_fPostalizzabile: res.masterOut.fPostalizzabile,
        master_out_sStatoSpedizione: res.masterOut.sStatoSpedizione,
        master_out_sComuneSpedizione: res.masterOut.sComuneSpedizione,
        master_out_sFrazioneSpedizione: res.masterOut.sFrazioneSpedizione,
        master_out_sCivicoAltro: res.masterOut.sCivicoAltro,
        master_out_sCap: res.masterOut.sCap,
        master_out_sPresso: res.masterOut.sPresso,
        master_out_sViaCompletaSpedizione: res.masterOut.sViaCompletaSpedizione
    };
}

function checkNormalizerItem({normalizer}) {
    const { batchId, oldFileKey, oldOutputFileKey, newFileKey, newOutputFileKey } = normalizer;

    if ([oldFileKey, oldOutputFileKey, newFileKey, newOutputFileKey].every(v => v === null)) {
      console.log(`[${batchId}] Fase di inizializzazione del batch.`);
      return null;
    }

    const inputChanged = oldFileKey !== newFileKey;
    const outputChanged = oldOutputFileKey !== newOutputFileKey;

    if (!inputChanged && !outputChanged) {
      console.log(`[${batchId}] No changes detected`);
      return null;
    }

    if (inputChanged && newFileKey) {
      console.log(`[${batchId}] Input changed → "${newFileKey}"`);
      return { type: 'NORMALIZER_REQUEST', fileKey: newFileKey };
    }

    if (outputChanged && newOutputFileKey) {
      console.log(`[${batchId}] Output changed → "${newOutputFileKey}"`);
      return { type: 'NORMALIZER_RESPONSE', fileKey: newOutputFileKey };
    }
    return null;
}

async function processNormalizerRequest(data, csvPayload) {
    const rows = await parseCsv(csvPayload);
    return rows.map((col) => {
        const [correlationId, requestCreatedAt, addressIdx] = (col[0] ?? '').split('#');
        let parsedAddressIdx = null;
        if (addressIdx !== undefined && addressIdx !== '') {
            const numericAddressIdx = Number(addressIdx);
            if (Number.isFinite(numericAddressIdx)) {
                parsedAddressIdx = numericAddressIdx;
            }
        }

        return {
            correlationId:       correlationId || null,
            service:             NORMALIZER_SERVICE,
            type:                REQUEST,
            batchId:             data.batchId ?? data.normalizer?.batchId ?? null,
            addressIdx:          parsedAddressIdx,
            requestCreatedAt:    requestCreatedAt || null,
            requestTimestamp:    new Date().toISOString(),

            idCodiceCliente:     col[0] ?? null,
            provincia:           col[1] ?? null,
            cap:                 col[2] ?? null,
            localita:            col[3] ?? null,
            localitaAggiuntiva:  col[4] || null,
            indirizzo:           col[5] ?? null,
            indirizzoAggiuntivo: col[6] || null,
            stato:               col[7] ?? null,
        };
    });
}

async function processNormalizerResponse(data, csvPayload) {
    const rows = await parseCsv(csvPayload);

    return rows.map((col) => {
        const [correlationId, responseCreatedAt, addressIdx] = (col[0] ?? '').split('#');
        let parsedAddressIdx = null;
        if (addressIdx !== undefined && addressIdx !== '') {
            const numericAddressIdx = Number(addressIdx);
            if (Number.isFinite(numericAddressIdx)) {
                parsedAddressIdx = numericAddressIdx;
            }
        }

        return {
            correlationId:           correlationId || null,
            service:                 NORMALIZER_SERVICE,
            type:                    RESPONSE,
            batchId:                 data.batchId ?? data.normalizer?.batchId ?? null,
            addressIdx:              parsedAddressIdx,
            responseCreatedAt:       responseCreatedAt || null,
            responseTimestamp:       new Date().toISOString(),
            nErroreNormDescription:  col[3] !== '' ? postelNErrorNormFromCode(col[3]) : null,

            id:                      col[0]  ?? null,
            nRisultatoNorm:          col[1]  !== '' ? Number(col[1]) : null,
            fPostalizzabile:         col[2]  !== '' ? Number(col[2]) : null,
            nErroreNorm:             col[3]  !== '' ? Number(col[3]) : null,
            sSiglaProv:              col[4]  || null,
            sStatoUff:               col[5]  || null,
            sStatoAbb:               col[6]  || null,
            sStatoSpedizione:        col[7]  || null,
            sComuneUff:              col[8]  || null,
            sComuneAbb:              col[9]  || null,
            sComuneSpedizione:       col[10] || null,
            sFrazioneUff:            col[11] || null,
            sFrazioneAbb:            col[12] || null,
            sFrazioneSpedizione:     col[13] || null,
            sCivicoAltro:            col[14] || null,
            sCap:                    col[15] || null,
            sPresso:                 col[16] || null,
            sViaCompletaUff:         col[17] || null,
            sViaCompletaAbb:         col[18] || null,
            sViaCompletaSpedizione:  col[19] || null,
        };
    });
}

module.exports = {
    buildDeduplicaRequestItem,
    buildDeduplicaResponseItem,
    checkNormalizerItem,
    processNormalizerRequest,
    processNormalizerResponse
};
