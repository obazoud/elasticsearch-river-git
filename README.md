### Git River for Elasticsearch

Welcome to the Git River Plugin for ["Elasticsearch"](http://www.elasticsearch.org)

### Versions

<table>
  <tr>
    <th>Confluence River Plugin</th><th>ElasticSearch</th>
  </tr>
  <tr>
    <td>master (0.0.1)</td><td>master (0.90.3)</td>
  </tr>
</table>

### Getting Started

#### Installation

Just type :

<pre>
$ bin/plugin -install obazoud/elasticsearch-river-git/0.0.1
-> Installing obazoud/elasticsearch-river-git/0.0.1...
Trying https://github.com/downloads/obazoud/elasticsearch-river-git-0.0.1.zip...
Downloading ...DONE
Installed git-river
</pre>

#### Creating a Git river

We create first an index to store data

<pre>
$ curl -XPUT 'localhost:9200/git/' -d '{}'
</pre>

We create the river with the following properties :

* Project name : obazoud-elasticsearch
* Git uri: git://github.com/obazoud/elasticsearch.git

<pre>
$ curl -XPUT 'localhost:9200/_river/git/_meta' -d '{
  "type": "git",
  "git": {
   "name": "obazoud-elasticsearch",
   "uri": "git://github.com/obazoud/elasticsearch.git",
   "issue_regex": "#(\\d*)",
   "indexing_diff": false,
   "update_rate": 1800000
  }
}'
</pre>

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
