
Questo il caricamento di produzione al 20231013
```
./dynamoDBLoadBatch.sh -t pn-addressManager-Cap -i production-cap-20231013.json -p sso_pn-confinfo-dev
./dynamoDBLoadBatch.sh -t pn-addressManager-Country -i production-country-20231013.json -p sso_pn-confinfo-dev
```


Per gli ambienti NON produttivi, a fini di test, aggiungere queste entry
```
./dynamoDBLoadBatch.sh -t pn-addressManager-Cap -i test-cap-20231013.json -p sso_pn-confinfo-dev
./dynamoDBLoadBatch.sh -t pn-addressManager-Country -i test-country-20231013.json -p sso_pn-confinfo-dev
```
