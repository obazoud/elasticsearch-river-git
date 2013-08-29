#!/bin/bash

set -e
mvn release:prepare -Dresume=false  -Pgpg
mvn release:perform -Pgpg

