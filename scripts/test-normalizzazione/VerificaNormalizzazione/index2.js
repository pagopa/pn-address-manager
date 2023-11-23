const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { DeleteCommand, ScanCommand, DynamoDBDocumentClient, QueryCommand } = require("@aws-sdk/lib-dynamodb");
const { fromSSO } = require("@aws-sdk/credential-provider-sso");
const fs = require('fs');
const path = require('path');
const csv = require('fast-csv');


const arguments = process.argv;
  
if(arguments.length<=4){
  console.error("Specify AWS profile")
  console.log("node index.js <aws-confinfo-profile> <correlation-prefix> <file>")
  process.exit(1)
}

const awsConfinfoProfile = arguments[2]
const prefix = arguments[3]
const file = arguments[4]

const start = Date.now()
console.log("Using AWS Confinfo profile: "+ awsConfinfoProfile)

const confinfoCredentials = fromSSO({ profile: awsConfinfoProfile })();
const confinfoDynamoDbClient = new DynamoDBClient({
    credentials: confinfoCredentials,
    region: 'eu-south-1'
});
const confinfoDDocClient = DynamoDBDocumentClient.from(confinfoDynamoDbClient);

const table = 'pn-addressManager-PNRequest'

let results = {
    ko: 0,
    okp: 0,
    oknp:0,
    kos: []
}


async function queryItemFromTable(correlationId){


    const params = {
        TableName: table,
        ExpressionAttributeValues: {
          ":correlationIdValue": correlationId
        },
        ExpressionAttributeNames: {
            "#correlationId": "correlationId"
        },
        KeyConditionExpression: "#correlationId = :correlationIdValue"
       
      };
    const ret = await confinfoDDocClient.send(new QueryCommand(params));
    if(ret && ret.Items){
        return ret.Items
    }
    return undefined
}


async function compare(row){

    let key = prefix + row[0]

    const items = await queryItemFromTable(key)

    if(items === undefined || items.length === 0) {
        console.error("Cannot find "+ key);
        return
    }

    fpost = row[2]
    addr = row[19]

    const item = items[0]

    if(fpost === '1'){
        if (item.message.includes(addr)) {
            results.okp+=1;
            console.log("OK [atteso postalizzabile]" + key);
        }else {
            results.ko+=1;
            console.log("----KO [atteso postalizzabile] " + key);
            console.log(row)
            console.log(item)
            console.log("----KO");
        }
    } else if (fpost === '0'){
        if (item.message.includes("Address declared ")) {
            results.oknp+=1;
            console.log("OK [atteso NON postalizzabile] " + key);
        }else {
            results.ko+=1;
            console.log("----KO [atteso NON postalizzabile]  " + key);
            console.log(row)
            console.log(item)
            console.log("----KO");
        }

    } else{
        console.error("-----------------------unexpected value " + fpost);
    }
}
    


function readcsv(){
//fs.createReadStream("/Users/federico.fatica/Work/PND/projects/_misc/verifica/PN_ADDRESSES_NORMALIZED.csv")

    return new Promise((resolve, reject) => {
        let map = {}
        fs.createReadStream(file)
        .pipe(csv.parse({ delimiter: ';' }))
        .on('error', error => reject)
        .on('data', row => {
            compare(row)
    
        })
        .on('end', rowCount => {
            console.log(`Parsed ${rowCount} rows`)
            resolve(map)
            });
        });
}



readcsv().then((map)=>{
    console.log("Done")

}) 
.catch((err)=>{
    console.error(err)
})

