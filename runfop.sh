#!/bin/sh
java -classpath classes:lib/commons-logging.jar:lib/fop.jar:lib/avalon.jar:lib/batik.jar net.sf.joost.Main -pdf "$@"
