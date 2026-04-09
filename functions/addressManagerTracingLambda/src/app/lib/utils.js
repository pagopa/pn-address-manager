const { parseCsv } = require('./csvUtils');
const { postelNErrorNormFromCode } = require('./postelNErrorNorm');

const DEDUPLICATE_SERVICE = 'DEDUPLICATE';
const NORMALIZER_SERVICE = 'NORMALIZER';
const REQUEST = 'REQUEST';
const RESPONSE = 'RESPONSE';

function buildDeduplicaRequestItem(req) {

    const correlationId = req?.masterIn?.id;
    if (!correlationId) {
        console.error("Missing correlationId: masterIn.id is null or undefined");
        return null;
    }

    return {
        correlationId,
        service: DEDUPLICATE_SERVICE,
        type: REQUEST,
        requestTimestamp: new Date().toISOString(),

        // SLAVE IN
        slave_in_id: req?.slaveIn?.id ?? null,
        slave_in_provincia: req?.slaveIn?.provincia ?? null,
        slave_in_cap: req?.slaveIn?.cap ?? null,
        slave_in_localita: req?.slaveIn?.localita ?? null,
        slave_in_localitaAggiuntiva: req?.slaveIn?.localitaAggiuntiva ?? null,
        slave_in_indirizzo: req?.slaveIn?.indirizzo ?? null,
        slave_in_indirizzoAggiuntivo: req?.slaveIn?.indirizzoAggiuntivo ?? null,
        slave_in_stato: req?.slaveIn?.stato ?? null,

        // MASTER IN
        master_in_id: req?.masterIn?.id ?? null,
        master_in_provincia: req?.masterIn?.provincia ?? null,
        master_in_cap: req?.masterIn?.cap ?? null,
        master_in_localita: req?.masterIn?.localita ?? null,
        master_in_localitaAggiuntiva: req?.masterIn?.localitaAggiuntiva ?? null,
        master_in_indirizzo: req?.masterIn?.indirizzo ?? null,
        master_in_indirizzoAggiuntivo: req?.masterIn?.indirizzoAggiuntivo ?? null,
        master_in_stato: req?.masterIn?.stato ?? null
    };
}

function buildDeduplicaResponseItem(res) {

    const correlationId = res?.masterOut?.id;
    if (!correlationId) {
        console.error("Missing correlationId: masterOut.id is null or undefined");
        return null;
    }

    const base = {
        correlationId,
        service: DEDUPLICATE_SERVICE,
        type: RESPONSE,
        responseTimestamp: new Date().toISOString(),
        risultatoDedu: res?.risultatoDedu ?? null,
        errore: res?.errore ?? null
    };

    const slaveOut = res?.slaveOut
        ? {
            slave_out_id: res.slaveOut.id ?? null,
            slave_out_nRisultatoNorm: res.slaveOut.nRisultatoNorm ?? null,
            slave_out_nErroreNorm: res.slaveOut.nErroreNorm ?? null,
            slave_out_nErroreNormDescription:
                res.slaveOut.nErroreNorm != null
                    ? postelNErrorNormFromCode(res.slaveOut.nErroreNorm)
                    : null,
            slave_out_sSiglaProv: res.slaveOut.sSiglaProv ?? null,
            slave_out_fPostalizzabile: res.slaveOut.fPostalizzabile ?? null,
            slave_out_sStatoUff: res.slaveOut.sStatoUff ?? null,
            slave_out_sStatoAbb: res.slaveOut.sStatoAbb ?? null,
            slave_out_sStatoSpedizione: res.slaveOut.sStatoSpedizione ?? null,
            slave_out_sComuneUff: res.slaveOut.sComuneUff ?? null,
            slave_out_sComuneAbb: res.slaveOut.sComuneAbb ?? null,
            slave_out_sComuneSpedizione: res.slaveOut.sComuneSpedizione ?? null,
            slave_out_sFrazioneUff: res.slaveOut.sFrazioneUff ?? null,
            slave_out_sFrazioneAbb: res.slaveOut.sFrazioneAbb ?? null,
            slave_out_sFrazioneSpedizione: res.slaveOut.sFrazioneSpedizione ?? null,
            slave_out_sCivicoAltro: res.slaveOut.sCivicoAltro ?? null,
            slave_out_sCap: res.slaveOut.sCap ?? null,
            slave_out_sPresso: res.slaveOut.sPresso ?? null,
            slave_out_sViaCompletaUff: res.slaveOut.sViaCompletaUff ?? null,
            slave_out_sViaCompletaAbb: res.slaveOut.sViaCompletaAbb ?? null,
            slave_out_sViaCompletaSpedizione: res.slaveOut.sViaCompletaSpedizione ?? null
        }
        : {};

    const masterOut = res?.masterOut
        ? {
            master_out_id: res.masterOut.id ?? null,
            master_out_nRisultatoNorm: res.masterOut.nRisultatoNorm ?? null,
            master_out_nErroreNorm: res.masterOut.nErroreNorm ?? null,
            master_out_nErroreNormDescription:
                res.masterOut.nErroreNorm != null
                    ? postelNErrorNormFromCode(res.masterOut.nErroreNorm)
                    : null,
            master_out_sSiglaProv: res.masterOut.sSiglaProv ?? null,
            master_out_fPostalizzabile: res.masterOut.fPostalizzabile ?? null,
            master_out_sStatoUff: res.masterOut.sStatoUff ?? null,
            master_out_sStatoAbb: res.masterOut.sStatoAbb ?? null,
            master_out_sStatoSpedizione: res.masterOut.sStatoSpedizione ?? null,
            master_out_sComuneUff: res.masterOut.sComuneUff ?? null,
            master_out_sComuneAbb: res.masterOut.sComuneAbb ?? null,
            master_out_sComuneSpedizione: res.masterOut.sComuneSpedizione ?? null,
            master_out_sFrazioneUff: res.masterOut.sFrazioneUff ?? null,
            master_out_sFrazioneAbb: res.masterOut.sFrazioneAbb ?? null,
            master_out_sFrazioneSpedizione: res.masterOut.sFrazioneSpedizione ?? null,
            master_out_sCivicoAltro: res.masterOut.sCivicoAltro ?? null,
            master_out_sCap: res.masterOut.sCap ?? null,
            master_out_sPresso: res.masterOut.sPresso ?? null,
            master_out_sViaCompletaUff: res.masterOut.sViaCompletaUff ?? null,
            master_out_sViaCompletaAbb: res.masterOut.sViaCompletaAbb ?? null,
            master_out_sViaCompletaSpedizione: res.masterOut.sViaCompletaSpedizione ?? null
        }
        : {};

    return {
        ...base,
        ...slaveOut,
        ...masterOut
    };
}
function checkNormalizerItem({normalizer}) {
    const { eventName, batchId, oldFileKey, oldOutputFileKey, newFileKey, newOutputFileKey } = normalizer;
    const outputChanged = oldOutputFileKey !== newOutputFileKey;
    if (eventName === "INSERT" && newFileKey) {
        console.log(`[${batchId}] Input changed → "${newFileKey}"`);
        return { type: 'NORMALIZER_REQUEST', fileKey: newFileKey };
    }else if(eventName === "MODIFY" && outputChanged && newOutputFileKey){
        console.log(`[${batchId}] Output changed → "${newOutputFileKey}"`);
        return { type: 'NORMALIZER_RESPONSE', fileKey: newOutputFileKey };
    }
    console.log(`[${batchId}] No changes detected`);
    return null;
}

async function processNormalizerRequest(data, csvPayload) {
    const rows = await parseCsv(csvPayload);
    return rows.map((col) => {
        const [correlationId, requestCreatedAt, addressIdx] = (col[0] ?? '').split('#');

        return {
            correlationId:       correlationId,
            service:             NORMALIZER_SERVICE,
            type:                REQUEST,
            batchId:             data.batchId ?? data.normalizer?.batchId ?? null,
            addressIdx:          addressIdx,
            requestCreatedAt:    requestCreatedAt,
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
        const [correlationId, requestCreatedAt, addressIdx] = (col[0] ?? '').split('#');

        return {
            correlationId:           correlationId,
            service:                 NORMALIZER_SERVICE,
            type:                    RESPONSE,
            batchId:                 data.batchId ?? data.normalizer?.batchId ?? null,
            addressIdx:              addressIdx,
            requestCreatedAt:        requestCreatedAt,
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
