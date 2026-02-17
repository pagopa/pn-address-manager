const { putRecordBatch } = require('./lib/firehose.js');
const { buildDeduplicaRequestItem, buildDeduplicaResponseItem } = require('./lib/utils.js');

exports.handleEvent = async (event) => {
    const DEDUPLICATE_REQUEST = "DEDUPLICATE_REQUEST";
    const DEDUPLICATE_RESPONSE = "DEDUPLICATE_RESPONSE";

    if (!event || !event.eventType) {
        console.error("EventType is required");
        return { success: false, error: "EventType is required" };
    }

    console.log("Received eventType:", event.eventType);

    if (!event.data) {
        console.error("Missing data in event body", event.data);
        return { success: false, error: "Missing data in event body" };
    }

    let itemsList = [];
    try {
        switch (event.eventType) {
            case DEDUPLICATE_REQUEST:
                itemsList.push(buildDeduplicaRequestItem(event.data));
                console.log("Successfully sent DEDUPLICATE_REQUEST to Firehose", { recordCount: itemsList.length });
                break;

            case DEDUPLICATE_RESPONSE:
                itemsList.push(buildDeduplicaResponseItem(event.data));
                console.log("Successfully sent DEDUPLICATE_RESPONSE to Firehose", { recordCount: itemsList.length });
                break;

            default:
                console.warn("Unknown eventType:", event.eventType);
                return { success: false, error: `Unknown eventType: ${event.eventType}` };
        }
        await putRecordBatch(itemsList);
        return { success: true };

    } catch (error) {
        console.error("Error processing event:", error);
        return { success: false, error: error.message };
    }

};
