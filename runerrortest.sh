#!/bin/csh -f
foreach i ( ../test/errors/error*.stx )
  echo -n `basename ${i}`: 
  run.sh build.xml ${i} |& sed -e 's/^[^:]*://; s/:-\?[0-9]*:/ -/' 
end
