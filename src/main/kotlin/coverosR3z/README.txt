The directories here are mainly separated by domain.  The entry-point to the application
is at Main.kt

Some interesting notes:

The server directory contains the code necessary to provide an interface on the web. Yes,
it's sockets and headers and threads and such.  This code is responsible for finding out
where to turn for calculations amongst the remainder of the system, so it will call out
throughout the other domains.  Check out ServerUtilities and SocketCommunication for the
juicy details.

Our database is very simple - Look for PureMemoryDatabase.  It's simply data in sets, with
a slew of methods to operate on that data.

The domainobjects directory holds a lot of data classes that act as typed containers for
business needs throughout the system.  We have done our level best to wrap raw primitives
as typed data as close as possible to the user interface, and then keep everything typed
all the way down to the database.


