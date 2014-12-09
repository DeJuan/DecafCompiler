#!/bin/bash

./build.sh
run_times="${1:-1}"
rm -f output_derby.txt
for ((i=0; i<run_times; i++))
do
  tests/derby/test.sh >> output_derby.txt
done
python parse_derby_results.py
