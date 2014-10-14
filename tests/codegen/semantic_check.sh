#!/bin/sh

runsemantics() {
  $(git rev-parse --show-toplevel)/run.sh -t inter $1
}

fail=0

for file in `dirname $0`/input/*.dcf; do
  if ! runsemantics $file; then
    echo "Legal file $file failed to pass semantic checks.";
    fail=1
  fi
done

for file in `dirname $0`/custom_tests/*.dcf; do
  if ! runsemantics $file; then
    echo "Legal file $file failed to pass semantic checks.";
    fail=1
  fi
done

exit $fail;
