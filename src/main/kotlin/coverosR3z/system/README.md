The _system_ package is for those cross-cutting aspects that affect the system
as a whole.

For example, 
* the code for handling command-line options for starting the system
* logging
* Helper functions for reading files

and so on.

This package is also home to the code that specifically is in charge of starting
the whole system, which the main function calls into at the onset.