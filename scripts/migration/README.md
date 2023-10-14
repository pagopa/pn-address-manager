echo "Usage: $(basename $0) -t <table_name> -i <input_file_name> [-p <aws_profile>] [-r <aws_region>]"


Questo il caricamento di produzione al 20231013
```
./dynamoDBLoad.sh -t pn-addressManager-Cap -i production-cap-20231013.json -p sso_pn-confinfo-dev
./dynamoDBLoad.sh -t pn-addressManager-Country -i production-country-20231013.json -p sso_pn-confinfo-dev
```


Per gli ambienti NON produttivi, a fini di test, aggiungere queste entry
```
./dynamoDBLoad.sh -t pn-addressManager-Cap -i test-cap-20231013.json -p sso_pn-confinfo-dev
./dynamoDBLoad.sh -t pn-addressManager-Country -i test-country-20231013.json -p sso_pn-confinfo-dev
```
