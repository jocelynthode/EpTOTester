#!/usr/bin/env bash

FOLDER=$1

if [ -z "$FOLDER" ]
  then
    echo "you have to indicate a folder without the trailing slash"
    exit
fi

FILE=$(ls $FOLDER | head -n 1)
BASE=$(awk '/Delivered/  {print $NF}' "$FOLDER/$FILE")
echo "Checking folder $FOLDER with base file $FILE"

for i in ${FOLDER}/172.*.txt
	do diff -q <(echo "$BASE") <(awk '/Delivered/  {print $NF}' "$i") || (echo "$i"; exit)
done
echo "OK"
