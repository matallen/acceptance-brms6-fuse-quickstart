#!/bin/bash

#===  CONFIGURATION  ====
REMOTE_REPO_ID=nexus
REMOTE_REPO_URL=http://localhost:9000/nexus/content/repositories/releases
#========================

LOCALLY=$(find . -name "jboss-fuse*.zip")
if [[ "x" != "x$1" ]]; then
    echo "Installing specified fuse: "+$1
    FUSE_PATH=$1
elif [[ "x" != "x$LOCALLY" ]]; then
    echo "Found fuse in local folder: $LOCALLY"
    echo "Install this (Y/N)?"
    read INSTALL
    if [[ "Y" == "$INSTALL" ]]; then
      FUSE_PATH=$LOCALLY
    else
      echo "Aborting"
      exit
    fi
else
    echo "Please download JBoss Fuse from the Red Hat Portal: https://access.redhat.com/products/red-hat-jboss-fuse"
    echo "Then use this script like this:"
    echo -e "\n./install-jboss-fuse-remotely.sh <absolute path to jboss-fuse.zip file>"
fi


FILENAME=$(echo $FUSE_PATH | grep -oE "(jboss-fuse-([a-z]+)-([a-z0-9.-]+).zip)")
FUSE_ARTIFACT=$(echo $FILENAME | grep -oE "(jboss-fuse-[a-z]+)")
FUSE_VERSION=$(echo $FILENAME | grep -oE "[0-9].+[^.zip]")

echo "Installing \"${FUSE_ARTIFACT}\" version \"${FUSE_VERSION}\" to a remote repository"

mvn deploy:deploy-file -DrepositoryId=${REMOTE_REPO_ID} -Durl=${REMOTE_REPO_URL} -Dfile=$FUSE_PATH -DgroupId=org.jboss.fuse -DartifactId=${FUSE_ARTIFACT} -Dversion=${FUSE_VERSION} -Dpackaging=zip -DgeneratePom=true

