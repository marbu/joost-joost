#!/bin/csh -f
# $Id: runerrortest.sh,v 1.5 2004/09/17 17:51:33 obecker Exp $
# Runs a set of error cases and checks the expected error message
# (This error message is always in line 3 of the input file)

# Temporary file for the error message
set tmp = err$$.tmp
rm -f err[0-9]*.tmp

if ( "$1" != "" ) then
  set files = ../test/errors/error$1.stx
  if ( ! -f ${files} ) then
    echo File ${files} not found
    exit
  endif
else
  set files = ../test/errors/error*.stx
endif

foreach i ( ${files} )
  echo -n `basename ${i}`: 
  run.sh build.xml ${i} |& sed -e '/^[^/]/d; /^$/d; s/^[^:]*://; s/:-\?[0-9]*:/:/' | tee ${tmp}
  sed -e '1,2d; 4,$d' < ${i} | diff ${tmp} -
  rm -f ${tmp}
end
