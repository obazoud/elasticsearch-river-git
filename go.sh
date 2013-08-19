#!/bin/bash
set -e
set -x

ES_HOME="/opt/elasticsearch"

mvn clean install -q $@
mkdir -p $ES_HOME/plugins/elasticsearch-river-git
rm -rf $ES_HOME/plugins/elasticsearch-river-git/*
unzip target/releases/elasticsearch-river-git-0.0.1-SNAPSHOT.zip -d $ES_HOME/plugins/elasticsearch-river-git

rm -rf $ES_HOME/data
rm -rf ~/.elasticsearch-river-git
/opt/elasticsearch/bin/elasticsearch -f -Xmx2g -Xms2g -Des.index.storage.type=memory
