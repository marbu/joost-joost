#!/bin/sh
# $Id: run.sh,v 1.2 2002/11/18 10:25:51 obecker Exp $
# runs joost
cp=$CLASSPATH
# include all jars in the lib directory
for i in lib/*.jar; do
    cp=$cp:$i
done
java -classpath classes:$cp net.sf.joost.Main "$@"

