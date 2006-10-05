#!/bin/sh
# $Id: runtest.sh,v 1.7 2006/10/05 10:42:06 obecker Exp $
# Performs a chosen set of transformations and compares the output with
# an expected result

# The directory containing all of the test cases
testdir=../test

# Temporary file for the transformation result
tmp=res$$.tmp
rm -f res[0-9]*.tmp

# Are there command line arguments which denote the test cases?
if [ -n "$1" ]; then
    # yes, run test just for these specific files
    while [ -n "$1" ]; do
        files="$files ${testdir}/$1.res"
        shift
    done
else
    # no, run test for all files in the test directory
    files=${testdir}/*.res
fi

for i in ${files}; do
    bn=`dirname $i`/`basename $i .res`
    echo `basename ${bn}`
    # Check if there is a file with a parameter specification
    if [ -f "${bn}.par" ]; then
        para=`cat ${bn}.par`
    else
        para=
    fi
    # Determine the input source file
    # This script supports the use of several stylesheets with a single
    # XML source.
    # If the basename contains a hyphen then the source may be named
    # "part before the hyphen" + ".xml"
    # Is there a source with the full name (including the hyphen)?
    if [ -f "${bn}.xml" ]; then
        source=${bn}.xml
    else
        source=`expr ${bn} : '\([^-]*\).*'`.xml
    fi
    # run and compare
    if [ -f "${source}" ]; then
        `dirname $0`/run.sh ${source} ${bn}.stx -o ${tmp} ${para} && diff ${tmp} ${bn}.res
        rm -f ${tmp}
    else
        echo No XML source found for `basename ${bn}`
    fi
done
