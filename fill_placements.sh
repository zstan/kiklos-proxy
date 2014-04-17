#!/bin/sh

redis-cli HSET "\".placements\"" "\"/111\"" "[\"java.util.ArrayList\",[\"/ptv?id=2504637\",\"some comment1\"]]"
redis-cli HSET "\".placements\"" "\"/112\"" "[\"java.util.ArrayList\",[\"/ptv?id=2504635\",\"some comment2\"]]"
redis-cli HSET "\".placements\"" "\"/222\"" "[\"java.util.ArrayList\",[\"/ptv?id=2504636\",\"some comment3\"]]"