#! /bin/bash 

_BASEURI="http://localhost:8887"
_FILE=$1
_PREFIX=$2
CXID=pn-mock
APIKEY=pn-mock-apikey


while IFS=";" read -r id pr cap city city2 addressRow addressRow2 country
do


  COMMAND="curl -v --location \"$_BASEURI/address-private/normalize\" \
  --header \"pn-address-manager-cx-id: ${CXID}\" \
  --header \"x-api-key: $APIKEY\" \
  --header 'Content-Type: application/json' \
  --data \"{
      \\\"correlationId\\\": \\\"$_PREFIX$id\\\",
      \\\"requestItems\\\": [
          {
              \\\"id\\\": \\\"1\\\",
              \\\"address\\\": {
                  \\\"addressRow\\\": \\\"$addressRow\\\",
                  \\\"addressRow2\\\": \\\"$addressRow2\\\",
                  \\\"cap\\\": \\\"$cap\\\",
                  \\\"city\\\": \\\"$city\\\",
                  \\\"city2\\\": \\\"$city2\\\",
                  \\\"pr\\\": \\\"$pr\\\",
                  \\\"country\\\": \\\"$country\\\"
              }
          }
      ]
  }\""

  echo $COMMAND | sh
#   echo $COMMAND

done < <(tail -n +2 $_FILE)



