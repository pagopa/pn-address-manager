const { putRecordBatch } = require('./lib/firehose.js');
const utils = require('./lib/utils.js');
const safeStorage = require('./lib/safeStorage');

const EVENT_TYPES = {
    DEDUPLICATE_REQUEST: "DEDUPLICATE_REQUEST",
    DEDUPLICATE_RESPONSE: "DEDUPLICATE_RESPONSE",
    NORMALIZER_REQUEST: "NORMALIZER_REQUEST",
    NORMALIZER_RESPONSE: "NORMALIZER_RESPONSE"
};

exports.handleEvent = async (event) => {
    if (!event?.eventType) {
        console.error("EventType is required");
        return { success: false, error: "EventType is required" };
    }

    if (!event?.data) {
        console.error("Missing data in event body");
        return { success: false, error: "Missing data in event body" };
    }

    let csvPayload;
    let { eventType, data } = event;

    try {
        if (eventType === "NORMALIZER") {
            const result = utils.checkNormalizerItem(event);
            if (!result) {
                console.log("Normalizer event skipped (no changes or init phase)");
                return { success: true, message: "Normalizer skipped" };
            }

            csvPayload = await safeStorage.downloadJson(result.fileKey);
            eventType = result.type;
        }

        const handlers = {
            [EVENT_TYPES.DEDUPLICATE_REQUEST]: () => [
                utils.buildDeduplicaRequestItem(data)
            ],

            [EVENT_TYPES.DEDUPLICATE_RESPONSE]: () => [
                utils.buildDeduplicaResponseItem(data)
            ],

            [EVENT_TYPES.NORMALIZER_REQUEST]: () =>
                utils.processNormalizerRequest(data, csvPayload),

            [EVENT_TYPES.NORMALIZER_RESPONSE]: () =>
                utils.processNormalizerResponse(data, csvPayload)
        };

        const handler = handlers[eventType];

        if (!handler) {
            console.warn("Unknown eventType:", eventType);
            return { success: false, error: `Unknown eventType: ${eventType}` };
        }

        const itemsList = handler();

        if (itemsList?.length) {
            await putRecordBatch(itemsList);
        }

        console.log(`Successfully processed ${eventType}`, {
            recordCount: itemsList?.length || 0
        });

        return { success: true };

    } catch (error) {
        console.error("Error processing event:", error);
        return { success: false, error: error.message };
    }
};