#!/bin/bash

cd /root/pub

kill $(ps aux | grep java.*system | grep -v 'grep' | awk '{print $2}')

kill $(ps aux | grep python | grep -v 'grep' | awk '{print $2}')

sleep 5s

rm -f /root/spring.txt

source /etc/profile

#nohup java -jar /root/powerjob-server-starter-4.3.9.jar > /dev/null 2>&1 &

#sleep 30s

nohup java -jar system-1.0-SNAPSHOT.jar > /root/spring.txt 2>&1 &

nohup python3.9 /root/bilibili-api/server.py > /dev/null 2>&1 &

