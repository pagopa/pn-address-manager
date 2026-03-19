const { postelNErrorNormFromCode } = require('../app/lib/postelNErrorNorm');
const assert = require("assert");

describe('postelNErrorNormFromCode', () => {
    it('should return correct description for known codes', () => {
        assert.strictEqual(postelNErrorNormFromCode(1), "IL CAP NON E' PRESENTE IN INPUT");
        assert.strictEqual(postelNErrorNormFromCode(101), "LA LOCALITA' NON E' PRESENTE IN INPUT");
        assert.strictEqual(postelNErrorNormFromCode(999), "CARATTERI NON ASCII PRESENTI NELL'INDIRIZZO");
    });

    it('should return generic error for unknown codes', () => {
        assert.strictEqual(postelNErrorNormFromCode(12345), "ERRORE GENERICO");
        assert.strictEqual(postelNErrorNormFromCode(-1), "ERRORE GENERICO");
    });

    it('should return null for undefined, null, 0, "" or generic error for string input', () => {
        assert.strictEqual(postelNErrorNormFromCode(undefined), null);
        assert.strictEqual(postelNErrorNormFromCode(null), null);
        assert.strictEqual(postelNErrorNormFromCode(0), null);
        assert.strictEqual(postelNErrorNormFromCode("0"), null);
        assert.strictEqual(postelNErrorNormFromCode('000'), null);
        assert.strictEqual(postelNErrorNormFromCode(""), null);
        assert.strictEqual(postelNErrorNormFromCode('abc'), "ERRORE GENERICO");
    });
});