#!/usr/bin/env bash

FILE=$1

if [ -z "$FILE" ]
  then
    echo "you have to indicate a base file"
    exit
fi

BASE=$(awk '/Delivered/  {print $NF}' "$FILE")

for i in 172.*.txt
    do diff -q <(echo "$BASE") <(awk '/Delivered/  {print $NF}' "$i") || echo "$i"
done
