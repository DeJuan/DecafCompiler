#!/bin/sh

gitroot=$(git rev-parse --show-toplevel)
./run.sh -t assembly $1 -o output.s
gcc output.s -o output.out
./output.out
