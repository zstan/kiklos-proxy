#!/bin/sh

#smartlabs
redis-cli HSET "\".placements\"" "\"111\"" "[\"java.util.ArrayList\",[\"http://v.adfox.ru/{account}/getCode?pp=efi&ps=byof&p2=eyit&pfc=a&pfb=a&plp=a&pli=a&pop=a&pct=c&puid5=1&puid25=1\"]]"
#bcs
redis-cli HSET "\".placements\"" "\"112\"" "[\"java.util.ArrayList\",[\"http://ib.adnxs.com/ptv?id=2504637\"]]"
#asg1
redis-cli HSET "\".placements\"" "\"113\"" "[\"java.util.ArrayList\",[\"http://asg.vidigital.ru/1/50006/c/v/2\"]]"
#hz1
redis-cli HSET "\".placements\"" "\"114\"" "[\"java.util.ArrayList\",[\"http://ads.adfox.ru/216891/getCodeTest?p1=blhhs&p2=euhw&pfc=a&pfb=a&plp=a&pli=a&pop=a\"]]"

redis-cli HSET "\".placements\"" "\"211\"" "[\"java.util.ArrayList\",[\"http://asg.vidigital.ru/1/50006/c/v/2\", \"http://asg.vidigital.ru/1/50006/c/v/2\"]]"

redis-cli HSET "\".placements\"" "\"212\"" "[\"java.util.ArrayList\",[\"http://ads.adfox.ru/216891/getCode?p1=bpvvp&p2=euhw&pfc=a&pfb=a&plp=a&pli=a&pop=a\", \"http://ads.adfox.ru/216891/getCode?p1=bpvvo&p2=euhw&pfc=a&pfb=a&plp=a&pli=a&pop=a\"]]"

redis-cli HSET "\".placements\"" "\"333\"" "[\"java.util.ArrayList\",[\"http://ads.adfox.ru/216891/getCode?p1=bpvvq&p2=euhw&pfc=a&pfb=a&plp=a&pli=a&pop=a\", \"http://ads.adfox.ru/216891/getCode?p1=bpvvo&p2=euhw&pfc=a&pfb=a&plp=a&pli=a&pop=a\"]]"

redis-cli HSET "\".durations\"" "5" "\"http://ads.adfox.ru/216891/getCode?p1=bpvvo&p2=euhw&pfc=a&pfb=a&plp=a&pli=a&pop=a\""
redis-cli HSET "\".durations\"" "10" "\"http://ads.adfox.ru/216891/getCode?p1=bpvvp&p2=euhw&pfc=a&pfb=a&plp=a&pli=a&pop=a\""
redis-cli HSET "\".durations\"" "15" "\"http://ads.adfox.ru/216891/getCode?p1=bpvvq&p2=euhw&pfc=a&pfb=a&plp=a&pli=a&pop=a\""
redis-cli HSET "\".durations\"" "20" "\"http://ads.adfox.ru/216891/getCode?p1=bpvvr&p2=euhw&pfc=a&pfb=a&plp=a&pli=a&pop=a\""
redis-cli HSET "\".durations\"" "25" "\"http://ads.adfox.ru/216891/getCode?p1=bpvvs&p2=euhw&pfc=a&pfb=a&plp=a&pli=a&pop=a\""
#redis-cli HSET "\".durations\"" "30" "\"http://ads.adfox.ru/216891/getCode?p1=bpvxm&p2=euhw&pfc=a&pfb=a&plp=a&pli=a&pop=a\""
#redis-cli HSET "\".durations\"" "35" "\"http://ads.adfox.ru/216891/getCode?p1=bpvyo&p2=euhw&pfc=a&pfb=a&plp=a&pli=a&pop=a\""

#redis-cli HSET "\".placements\"" "\"212\"" "[\"java.util.ArrayList\",[\"\", \"\", \"\"]]"
#redis-cli HSET "\".placements\"" "\"212\"" "[\"java.util.ArrayList\",[\"http://asdasdfasdf.com/\", \"http://asg.vidigital.ru/1/50006/c/v/2\", \"http://ib.adnxs.com/ptv?id=2504637\"]]"
#redis-cli HSET "\".placements\"" "\"222\"" "[\"java.util.ArrayList\",[\"33445566\",\"some comment1\"]]"