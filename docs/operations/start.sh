#!/bin/sh

MYDIR="$(dirname "$(readlink -f "$0")")"
cd $MYDIR
java -jar r3z-1.2.jar &>>r3z.log &
echo $! > pid
cd -
