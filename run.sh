#!/bin/bash

cd /root/pub

kill $(ps aux | grep java | grep -v 'grep' | awk '{print $2}')

kill $(ps aux | grep python | grep -v 'grep' | awk '{print $2}')

sleep 5s

rm -f /root/spring.txt

source /etc/profile

nohup java -jar system-1.0-SNAPSHOT.jar > /root/spring.txt 2>&1 &

nohup python3.9 /root/bilibili-api/server.py > /dev/null 2>&1 &

