#!/bin/sh
for i in ../test/errors/*.stx; do
  echo `basename ${i}`
  run.sh build.xml ${i} > /dev/null
done
