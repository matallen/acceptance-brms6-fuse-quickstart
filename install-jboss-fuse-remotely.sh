#!/bin/sh

mvn deploy:deploy-file -DrepositoryId=nexus -Durl=http://localhost:9000/nexus/content/repositories/releases -Dfile=/home/mallen/Work/platforms/jboss-fuse-minimal-6.1.1.redhat-412.zip -DgroupId=org.jboss.fuse -DartifactId=jboss-fuse-minimal -Dversion=6.1.1.redhat-412 -Dpackaging=zip -DgeneratePom=true
