#!/usr/bin/env bash

FOLDER=$1
FILE=$2


if [ -z "$FOLDER" ]
  then
    echo "you have to indicate a folder without the trailing slash"
    exit
fi
if [ -z "$FILE" ]
  then
    echo "you have to indicate a base file"
    exit
fi

BASE=$(awk '/Delivered/  {print $NF}' "$FOLDER/$FILE")

for i in $FOLDER/172.*.txt
    do diff -q <(echo "$BASE") <(awk '/Delivered/  {print $NF}' "$i") || echo "$i"
done
