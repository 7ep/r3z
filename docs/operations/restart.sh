#!/bin/sh

# get the directory where this file is located, so we can jump to it
MYDIR="$(dirname "$(readlink -f "$0")")"

# jump to it
cd $MYDIR
OLD_PID=$(cat pid)

# stop and run the timekeeping application
kill $OLD_PID; java -jar r3z.jar -p 12345 -d db &>>r3z.log &

# get the process id, pop it in a file (we'll use this to stop the process later)
echo $! > pid

# try killing the old process again
# for some reason it doesn't die right away, sometimes
kill $OLD_PID

# jump back to where we started
cd -
