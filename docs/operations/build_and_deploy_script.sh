#!/bin/sh

scp build/libs/r3z-1.2.jar byron@renomad.com:~/r3z && ssh byron@renomad.com "~/r3z/stop.sh && sleep 2 && ~/r3z/start.sh"