#!/bin/sh
java -classpath classes:lib/log4j.jar:lib/fop.jar:lib/avalon.jar:lib/batik.jar net.sf.joost.Main -pdf "$@"
