const csv = require('csv-parser');
const { Readable } = require('stream');

/**
 * Parsa un CSV con separatore ';' e logica posizionale (nessun header).
 */
function parseCsv(csvPayload) {
    return new Promise((resolve, reject) => {
        const records = [];
        const stream = Readable.from([csvPayload]);

        stream
            .pipe(csv({
                separator: ';',
                headers: false,   // logica posizionale, niente header
                skipComments: true
            }))
            .on('data', (row) => {
                records.push(Object.values(row));
            })
            .on('end', () => resolve(records))
            .on('error', reject);
    });
}

module.exports = {
    parseCsv
};