### Git River for Elasticsearch

Welcome to the Git River Plugin for ["Elasticsearch"](http://www.elasticsearch.org)

### Versions

<table>
  <tr>
    <th>Git River Plugin</th><th>ElasticSearch</th>
  </tr>
  <tr>
    <td>master (0.0.x)</td><td>master (0.90.3)</td>
  </tr>
</table>

### Getting Started

#### Installation

Just type :

<pre>
% bin/plugin --install com.bazoud.elasticsearch/elasticsearch-river-git/0.0.1
-> Installing com.bazoud.elasticsearch/elasticsearch-river-git/0.0.1...
Trying http://download.elasticsearch.org/com.bazoud.elasticsearch/elasticsearch-river-git/elasticsearch-river-git-0.0.1.zip...
Trying http://search.maven.org/remotecontent?filepath=com/bazoud/elasticsearch/elasticsearch-river-git/0.0.1/elasticsearch-river-git-0.0.1.zip...
Downloading ..............DONE
Installed river-git into /path/to/elasticsearch/plugins/river-git
</pre>

#### Creating a Git river

We create first an index to store data

<pre>
$ curl -XPUT 'localhost:9200/git/' -d '{}'
</pre>

We create the river with the following properties :

* Project name (it is supposed to be unique): obazoud-elasticsearch
* Git uri: git://github.com/obazoud/elasticsearch.git

<pre>
$ curl -XPUT 'localhost:9200/_river/git/_meta' -d '{
  "type": "git",
  "git": {
   "name": "obazoud-elasticsearch",
   "uri": "git://github.com/obazoud/elasticsearch-river-git.git",
   "issue_regex": "#(\\d*)",
   "indexing_diff": false,
   "update_rate": 1800000
  }
}'
</pre>

### Build

[![Build Status](https://buildhive.cloudbees.com/job/obazoud/job/elasticsearch-river-git/badge/icon)](https://buildhive.cloudbees.com/job/obazoud/job/elasticsearch-river-git/)

### LICENSE

```
This software is licensed under the Apache 2 license, quoted below.

Copyright 2013 Olivier BAZOUD

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

See [LICENSE](LICENSE.txt)
