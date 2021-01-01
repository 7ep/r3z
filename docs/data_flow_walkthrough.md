start the server
----------------

1. Check to see what command-line options have been sent, if any.  
   If there are some, figure out what they mean and set the proper configuration on the server

2. Start the database.  This most commonly will read the files 
   from the disk and create the data collections in memory.  

3. Instantiate the various utility files, which require the 
   database


Handle a request
----------------

1. The server will be waiting for a connection.  When a client calls
    in, a single procedure will be called to completely handle the
   client's request.  This procedure is wrapped in a thread and
   handled in a thread pool.  (By the way, as of this time of
   writing, it made more sense to go with a thread pool versus
   asynchronous approaches like coroutines because of the 
   simplicity it achieves.)
2. The processing will descend down into the depths of the code,
    from the server to the API to the Utility, to the Persistence,
   to the Database, and back again.  Since most of this is
   following a procedural paradigms, it is pretty easy to follow
   it down and back.



