const { handleEvent } = require("../app/eventHandler.js");
const { expect } = require('chai');

describe('Lambda handler', () => {
  it('should return success for a valid event', async () => {
    const event = {};
    const context = {};
    const result = await handleEvent(event, context);

    expect(result).to.equal(true);
  });
});