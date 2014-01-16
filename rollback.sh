#!/bin/sh

if [ $# -ne 1 ]; then
    echo "Usage: $0 (tag name)"
    exit 1
fi

mvn release:rollback
svn delete -m "rolling back $1" https://maven-replacer-plugin.googlecode.com/svn/tags/$1
