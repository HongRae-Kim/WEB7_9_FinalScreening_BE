#!/bin/sh
set -eu

RUN_ID=${RUN_ID:-$(date +%Y%m%d_%H%M%S)}
export RUN_ID
mkdir -p /work/k6/results

exec k6 run \
  -o experimental-prometheus-rw \
  --summary-export=/work/k6/results/${RUN_ID}.json \
  /work/k6/main.js
