#!/bin/sh
PATH=/home/mallen/Work/platforms/jboss-fuse-minimal-6.1.1.redhat-412.zip
mvn install:install-file -DgroupId=org.jboss.fuse -DartifactId=jboss-fuse-minimal -Dversion=6.1.1.redhat-412 -Dpackaging=zip -Dfile=$PATH
