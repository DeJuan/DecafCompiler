#!/bin/bash

runparser() {
  $(git rev-parse --show-toplevel)/run.sh -t parse $1
}

fail=0

for file in `dirname $0`/illegal/*.dcf; do
  if ! runparser $file; then
    echo "Legal file $file failed to parse.";
    fail=1
  fi
done

for file in `dirname $0`/legal/*.dcf; do
  if ! runparser $file; then
    echo "Legal file $file failed to parse.";
    fail=1
  fi
done

exit $fail;
