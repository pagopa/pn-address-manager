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

    it('should return generic error for undefined, null, or string input', () => {
        assert.strictEqual(postelNErrorNormFromCode(undefined), "ERRORE GENERICO");
        assert.strictEqual(postelNErrorNormFromCode(null), "ERRORE GENERICO");
        assert.strictEqual(postelNErrorNormFromCode('abc'), "ERRORE GENERICO");
    });
});