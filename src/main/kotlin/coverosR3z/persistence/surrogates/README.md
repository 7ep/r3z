### Surrogates

_Surrogate (noun): a substitute, especially a person deputizing for another in a specific role or office_

These classes represent simplified versions of data for the
purpose of writing to disk.

For example, the TimeEntry class is strongly typed and therefore
contains a lot of data.  Here's a toString for TimeEntry:

    TimeEntry(
        id=1, 
        employee=Employee(id=EmployeeId(value=1), name=EmployeeName(value=DefaultEmployee)), 
        project=Project(id=ProjectId(value=1), name=ProjectName(value=Default_Project)), 
        time=Time(numberOfMinutes=60), 
        date=Date(epochDay=18438, 2020-06-25), 
        details=Details(value=))

And here is the surrogate version:

    { i: 1 , e: 1 , p: 1 , t: 60 , d: 18438 , dtl:  }

By separating these, it is also easily possible to pick and choose how you 
want to store data.  There might be multiple classes that combine, or perhaps we
only need to persist certain data but not all.