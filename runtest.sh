#!/bin/sh
for i in ../test/*.res; do
  bn=`dirname $i`/`basename $i .res`
  echo `basename $bn`
  run.sh ${bn}.xml ${bn}.stx -o xxx.tmp
  diff xxx.tmp ${bn}.res
done
rm xxx.tmp