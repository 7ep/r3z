The directories here are mainly separated by domain.  The entry-point to the application
is at Main.kt

Some interesting notes:

The server directory contains the code necessary to provide
an interface on the web - sockets and headers and
threads and such.  This code is responsible for finding out
where to turn for calculations amongst the remainder of the
system, so it will call out throughout the other domains.
Check out ServerUtilities and Server for the
juicy details.


Our database is very simple - Look for PureMemoryDatabase.
It's simply data in sets, with a slew of methods to operate
on that data.



