#!/bin/sh

# get the directory where this file is located, so we can jump to it
MYDIR="$(dirname "$(readlink -f "$0")")"

# jump to it
cd $MYDIR

# the following presumes there is a keystore in the same directory as r3z.jar.
# Check "convert_lets_encrypt_certs_to_keystore.txt" in docs/operations for a
# bit more info on this

# Using the Xss180k means that each thread uses 180 kb of memory.  This is just to
# minimize memory use where we can

# run the timekeeping application
java -Djavax.net.ssl.keyStore=keystore -Djavax.net.ssl.keyStorePassword=passphrase -Xss180k -jar r3z.jar -h renomad.com  &>>r3z.log &

# get the process id, pop it in a file (we'll use this to stop the process later)
echo $! > pid

# jump back to where we started
cd -
