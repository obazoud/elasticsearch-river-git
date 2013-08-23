#!/bin/bash
set -e
set -x

ES_HOME="/opt/elasticsearch"

mvn clean install -q $@
mkdir -p $ES_HOME/plugins/river-git
rm -rf $ES_HOME/plugins/river-git
unzip target/releases/elasticsearch-river-git-*.zip -d $ES_HOME/plugins/river-git

rm -rf $ES_HOME/data
rm -rf ~/.elasticsearch-river-git
/opt/elasticsearch/bin/elasticsearch -f -Xmx2g -Xms2g -Des.index.storage.type=memory
