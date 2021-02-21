#!/bin/sh

# when exactly do we need to clean first?
# gradlew clean jar && scp build/libs/r3z.jar byron@renomad.com:~/r3z && ssh byron@renomad.com "~/r3z/stop.sh && sleep 2 && ~/r3z/start.sh"

gradlew jar && scp build/libs/r3z.jar r3z@renomad.com:~/r3z && ssh r3z@renomad.com "~/r3z/restart.sh && tail ~/r3z/r3z.log"