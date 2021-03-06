#!/usr/bin/env bash

export ADMIN_PASSWD="p@\$\$w0rd"
export JBOSS_ROOT_DIR=jboss

if [ "X${HOSTNAME}" == "X" ]; then
  export HOSTNAME=cadmium.localhost
fi

if [ "X${USERNAME}" == "X" ]; then
  export USERNAME=ubuntu
fi

JBOSS_DIST="jboss-as-7.1.1.Final"
JBOSS_URL="http://download.jboss.org/jbossas/7.1/${JBOSS_DIST}/${JBOSS_DIST}.zip"

if [[ "NOPARAM$1" != "NOPARAM" ]]; then
  JBOSS_URL="$1"
  JBOSS_DIST=${JBOSS_URL##*/}
  JBOSS_DIST=${JBOSS_DIST%%\.zip}
fi

set -e

cd $( dirname "${BASH_SOURCE[0]}" )

./install-dependencies.sh

if [ -e pre-provision ]; then
  SCRIPTS=$(ls pre-provision)
  for SCRIPT in $SCRIPTS; do
    . pre-provision/$SCRIPT
  done
fi

./download-jboss.sh "${JBOSS_URL}"

./install-jboss.sh "${JBOSS_DIST}" "init-jboss.sh"

./install-modules.sh

./install-configuration-repo.sh

./setup-loggly.sh

./install-shiro.sh

./install-apache.sh

./install-keys.sh

./install-cadmium-cli.sh

./setup-cadmium-admin-repo.sh

./install-cadmium.sh

./start-jboss.sh

./deploy-war.sh

if [ -e post-provision ]; then
  SCRIPTS=$(ls post-provision)
  for SCRIPT in $SCRIPTS; do
    . post-provision/$SCRIPT
  done
fi

set +e
echo "Done"