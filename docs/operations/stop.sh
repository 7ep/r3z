#!/bin/sh

# get the directory where this file is located, so we can jump to it
MYDIR="$(dirname "$(readlink -f "$0")")"

# jump to it
cd $MYDIR

# kill the timekeeping application by reading the process id from the "pid"
# file and "kill"-ing it (regular kill is nice.  we're not kill -9'ing here!)
kill $(cat pid)

# get rid of the now obsolete process id file
rm pid

# jump back to where we started
cd -