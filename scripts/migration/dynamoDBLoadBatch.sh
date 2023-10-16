#!/bin/bash

aws_profile=""
aws_region="eu-south-1"

while getopts 't:i:p:r:' opt ; do
  case "$opt" in 
    p)
      aws_profile=${OPTARG}
    ;;
    r)
      aws_region=${OPTARG}
    ;;
    t)
      tableName=${OPTARG}
    ;;
    i)
      inputFileName=${OPTARG}
    ;;
    :)
      echo -e "option requires an argument.\nUsage: $(basename $0) [-a] [-b] [-c arg]"
      exit 1
    ;;
    ?|h)
      echo "Usage: $(basename $0) -t <table_name> -i <input_file_name> [-p <aws_profile>] [-r <aws_region>]"
      exit 1
    ;;
  esac
done

if [[ ! $tableName || ! $inputFileName ]] ; then
  echo "both -t and -i parameters are mandatory"
  exit 1
fi

if [[ ! -f ${inputFileName} ]] ; then
  echo "invalid input file name ${inputFileName}"
  exit 1
fi

aws_command_base_args=""
if ( [ ! -z "${aws_profile}" ] ) then
  aws_command_base_args="${aws_command_base_args} --profile $aws_profile"
fi
if ( [ ! -z "${aws_region}" ] ) then
  aws_command_base_args="${aws_command_base_args} --region  $aws_region"
fi
echo ${aws_command_base_args}

lines=$(($(cat ${inputFileName} | wc -l)+1))
i=1
while [ $i -le $lines ]
do
  next_i=$(($i+25))
  echo "From $i to $next_i"


  BATCH=$(echo "{\"$tableName\": []}" | jq)

  cat $inputFileName  | awk "NR >= $i  && NR < $next_i { print }" > _tmp_data.json

  while read -r entry
  do

    ITEM=$(echo "{\"PutRequest\": {} }" | jq --argjson e "$entry" '.PutRequest.Item+=$e')
    
    if [ $tableName =  "pn-addressManager-Cap" ]; then
      BATCH=$(echo $BATCH | jq --argjson item "$ITEM" '."pn-addressManager-Cap" += [$item]' ) 
    elif [ $tableName =  "pn-addressManager-Country" ]; then
      BATCH=$(echo $BATCH | jq --argjson item "$ITEM" '."pn-addressManager-Country" += [$item]' ) 
    fi

    
  done < "_tmp_data.json"

  echo $BATCH > _tmp_data.json
  
  aws ${aws_command_base_args} \
     dynamodb batch-write-item \
     --request-items file://_tmp_data.json

  i=$next_i
  
  sleep 1
done
