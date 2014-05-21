#!/bin/sh

#smartlabs
redis-cli HSET "\".placements\"" "\"111\"" "[\"java.util.ArrayList\",[\"http://ib.adnxs.com/ptv?id=2504637\"]]"
#bcs
redis-cli HSET "\".placements\"" "\"112\"" "[\"java.util.ArrayList\",[\"http://ib.adnxs.com/ptv?id=2504637\"]]"
#asg1
redis-cli HSET "\".placements\"" "\"113\"" "[\"java.util.ArrayList\",[\"http://asg.vidigital.ru/1/50006/c/v/2\"]]"
#hz1
redis-cli HSET "\".placements\"" "\"114\"" "[\"java.util.ArrayList\",[\"http://ads.adfox.ru/216891/getCodeTest?p1=blhhs&p2=euhw&pfc=a&pfb=a&plp=a&pli=a&pop=a\"]

redis-cli HSET "\".placements\"" "\"222\"" "[\"java.util.ArrayList\",[\"http://asg.vidigital.ru/1/50006/c/v/2\", \"http://ib.adnxs.com/ptv?id=2504637\"]]"
#redis-cli HSET "\".placements\"" "\"222\"" "[\"java.util.ArrayList\",[\"33445566\",\"some comment1\"]]"
