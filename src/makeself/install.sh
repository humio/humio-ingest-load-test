#!/usr/bin/env bash
apt -y update
apt -y install default-jre-headless

HUMIO_PERFTEST_PURPOSE=$1
cp ${HUMIO_PERFTEST_PURPOSE}.service /etc/systemd/system/
mkdir -p /usr/lib/humio/ /etc/humio
cp perftest.jar /usr/lib/humio/

cp perftest.sh /usr/bin/
chmod 755 /usr/bin/perftest.sh

ENV_CONFIG_PATH=/etc/humio/${HUMIO_PERFTEST_PURPOSE}.env
rm -rf ${ENV_CONFIG_PATH}
touch ${ENV_CONFIG_PATH}
echo "HUMIO_BASEURLS=$2" >> ${ENV_CONFIG_PATH}
echo "HUMIO_INGEST_TOKEN=${HUMIO_INGEST_TOKEN}" >> ${ENV_CONFIG_PATH}
echo "HUMIO_TENS_GB_PER_DAY=${HUMIO_TENS_GB_PER_DAY:-10}" >> ${ENV_CONFIG_PATH}
echo "HUMIO_QUERY_USERS=${HUMIO_INGEST_USERS:-50}" >> ${ENV_CONFIG_PATH}

systemctl enable ${HUMIO_PERFTEST_PURPOSE}.service
systemctl restart ${HUMIO_PERFTEST_PURPOSE}.service
