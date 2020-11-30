#!/bin/sh

MYDIR="$(dirname "$(readlink -f "$0")")"
cd $MYDIR
kill $(cat pid)
rm pid
cd -
