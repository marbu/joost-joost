#!/bin/sh
# $Id: runtest.sh,v 1.2 2002/11/18 10:00:11 obecker Exp $
# performs a chosen set of transformations and compares the output with
# an expected result

# the directory containing all of the test cases
testdir=../test

# Is there a command line argument which denotes the test case?
if [ -n "$1" ]; then
# yes, run test just for this specific file
    files=${testdir}/$1.res
else
# no, run test for all files in the test directory
    files=${testdir}/*.res
fi

for i in ${files}; do
    bn=`dirname $i`/`basename $i .res`
    echo `basename $bn`
# check if there is a file with a parameter specification
    if [ -f "${bn}.par" ]; then
        para=`cat ${bn}.par`
    else
        para=
    fi
# run and compare
    run.sh ${bn}.xml ${bn}.stx -o xxx.tmp ${para}
    diff xxx.tmp ${bn}.res
done
rm xxx.tmp
