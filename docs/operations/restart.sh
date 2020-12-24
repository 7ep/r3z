#!/bin/sh

# get the directory where this file is located, so we can jump to it
MYDIR="$(dirname "$(readlink -f "$0")")"

# jump to it
cd $MYDIR

# stop and run the timekeeping application
kill $(cat pid); java -jar r3z-1.2.jar -p 12345 -d db &>>r3z.log &

# get the process id, pop it in a file (we'll use this to stop the process later)
echo $! > pid

# jump back to where we started
cd -
