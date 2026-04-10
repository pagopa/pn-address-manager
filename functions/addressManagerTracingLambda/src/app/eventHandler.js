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
    const records = event.Records ?? [event];
    const results = [];

    for (const record of records) {
        let parsedEvent;
        try {
            parsedEvent = record.body ? JSON.parse(record.body) : record;
        } catch (err) {
            console.error("Failed to parse record body:", err);
            throw new Error("Failed to parse record body");
        }

        const result = await processSingleEvent(parsedEvent);
        results.push(result);
    }
    return results;
};


const processSingleEvent = async (event) => {
    if (!event?.eventType) {
        console.error("EventType is required");
        throw new Error("EventType is required");
    }

    let { eventType } = event;
    let data = event.data;
    if (!data && event.normalizer) {
        data = { normalizer: event.normalizer };
    }
    if (!data) {
        console.error("Missing data in event body");
        throw new Error("Missing data in event body");
    }

    let csvPayload;

    try {
        if (eventType === "NORMALIZER") {
            const result = utils.checkNormalizerItem(event);
            if (!result) {
                console.log("Normalizer event skipped (no changes or init phase)");
                return { success: true, message: "Normalizer skipped" };
            }

            csvPayload = await safeStorage.downloadText(result.fileKey);
            eventType = result.type;
        }

        const handlers = {
            [EVENT_TYPES.DEDUPLICATE_REQUEST]: async () => [
                utils.buildDeduplicaRequestItem(data)
            ],

            [EVENT_TYPES.DEDUPLICATE_RESPONSE]: async () => [
                utils.buildDeduplicaResponseItem(data)
            ],

            [EVENT_TYPES.NORMALIZER_REQUEST]: async () =>
                utils.processNormalizerRequest(data, csvPayload),

            [EVENT_TYPES.NORMALIZER_RESPONSE]: async () =>
                utils.processNormalizerResponse(data, csvPayload)
        };

        const handler = handlers[eventType];

        if (!handler) {
            console.warn("Unknown eventType:", eventType);
            throw new Error("Unknown eventType");
        }

        const itemsList = await handler();

        const validItems = (itemsList ?? []).filter(item => item != null);

        if (validItems.length > 0) {
            await putRecordBatch(validItems);
        } else {
            console.warn(`No valid items to put for ${eventType}`);
        }

        console.log(`Successfully processed ${eventType}`, {
            recordCount: validItems.length,
            discardedCount: (itemsList?.length ?? 0) - validItems.length
        });

        return { success: true };

    } catch (error) {
        console.error("Error processing event:", event, error);
        throw new Error("Error processing event");
    }
};