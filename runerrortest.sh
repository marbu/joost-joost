#!/bin/sh
for i in ../test/errors/*.stx; do
  echo
  echo `basename ${i}`
  run.sh build.xml ${i} 2> xxx.tmp
  cat -n $i
  echo "----------------------------------------------------------------------"
  cat xxx.tmp
  echo "======================================================================"
done
rm xxx.tmp
