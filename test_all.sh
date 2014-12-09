#!/bin/bash

./build.sh
tests/codegen/test.sh
tests/codegen-hidden/test.sh
tests/optimizer/test.sh
tests/derby/test.sh
