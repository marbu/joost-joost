#!/bin/sh
# $Id: run.sh,v 1.3 2002/11/18 16:14:27 obecker Exp $
# runs joost

basedir=`dirname $0`
cp=$CLASSPATH
# include all jars in the lib directory
for i in $basedir/lib/*.jar; do
    cp=$cp:$i
done
java -classpath $basedir/classes:$cp net.sf.joost.Main "$@"

