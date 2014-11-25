#!/bin/sh

gitroot=$(git rev-parse --show-toplevel)
for i in "$@"; do
  echo "$i"
  ./run.sh -t assembly $i -o output.s
  #gcc output.s -o output.out
  #./output.out
done
