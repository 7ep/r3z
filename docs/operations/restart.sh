#!/bin/sh

# get the directory where this file is located, so we can jump to it
MYDIR="$(dirname "$(readlink -f "$0")")"

# jump to it
cd $MYDIR

# get the old process id
OLD_PID=$(cat pid)

# stop and run the timekeeping application
kill $OLD_PID

# When the system is running, a file is created called
# "SYSTEM_RUNNING" in the top directory.  It is set
# to be deleted when the virtual machine stops.  This way,
# we can tell the system is entirely shut down, so we can
# restart cleanly.

i=0
until [ ! -f SYSTEM_RUNNING ]
do
  # waits up to 10 seconds, then bails
  if [ $i -gt 10 ]
  then
    echo "It's been 10 seconds, bailing"
    exit 1
  fi
  echo "Waiting for system to shutdown... $i"
  ((i=i+1))
  sleep 1
done

echo "System has shutdown"

# the following presumes there is a keystore in the same directory as r3z.jar.
# Check "convert_lets_encrypt_certs_to_keystore.txt" in docs/operations for a
# bit more info on this

echo "Starting system"
java -Djavax.net.ssl.keyStore=keystore -Djavax.net.ssl.keyStorePassword=passphrase -jar r3z.jar -p 12345 -s 12443 -d db &>>r3z.log &

# get the process id, pop it in a file
# (we'll use this to stop the process later)
echo $! > pid

# Give the system time to start
sleep 2

# Check it's running
i=0
if [ ! -f SYSTEM_RUNNING ]
then
  echo
  echo
  echo "***************************************************************"
  echo "WARNING! The system does not appear to be running after startup"
  echo "         (Could not find a file called SYSTEM_RUNNING          "
  echo "***************************************************************"
  echo
fi

# jump back to where we started
cd -
