### r3z

_note_ This project is now archived and will receive no further modifications.  See 
https://github.com/byronka/r3z for continued updates.

This is an application by [Coveros](https://www.coveros.com/) to demonstrate good
software practices.  As we say in agile... _Working software over comprehensive 
documentation_ ... but that doesn't mean we can't have pretty good documentation too. 

#### Quick Start:

* Install [Java](https://www.java.com/en/download/) if you don't already have it.
* [Clone](https://git-scm.com/docs/git-clone) or [download](https://github.com/7ep/r3z/archive/master.zip) this repo. 
    * If you don't know what "clone" means here, just download it.
    * if you download, unzip the downloaded file to its own directory.
* On the command line in the project directory:
    * run `gradlew run` to run the server, then open https://localhost:12443/ (it uses a self-signed cert)
    * The first time the system is run, it will create an administrator account with
      username "administrator", and a password will be generated and shown in the log
      and stored in "admin_acct.txt" in the home directory.
    * As admin, you can create new employee accounts.  Copy the link on the account page
      and use it when logged out to register a new user as that employee.

#### Summary:

R3z consists of a web application and tests.  Its goals are: 

* To provide an ecosystem suitable for practicing agile engineering
* To enable research and innovation in valuable development techniques
* To meet business needs in a capabilities-oriented way, rather than being 
tool driven or following the more typical big-design-up-front waterfall methods
* To provide a demonstration of the results of deep agile internally, as well as for clients and partners

#### UI Tests:

[![A run of the UI tests](https://img.youtube.com/vi/mxdxYZWLbDQ/0.jpg)](https://www.youtube.com/watch?v=mxdxYZWLbDQ)

#### Pair programming:

[![pair programming with Matt with TDD](https://img.youtube.com/vi/XhZ4qBROY6I/0.jpg)](https://www.youtube.com/watch?v=XhZ4qBROY6I)
[![pair programming with Matt on generics](https://img.youtube.com/vi/FryXTzzm7ws/0.jpg)](https://www.youtube.com/watch?v=FryXTzzm7ws)

#### Getting started:

Check out the [development handbook](docs/development_handbook.md)

#### Theme

*What would happen if you built software in the simplest possible way from scratch?*

*What if our team held quality sacred?*

*What if we spent all the necessary time to think things through?*

*What if we incorporated diverse perspectives?*

*What if testing drove the design?*

*If we understand that our software is a reflection of our culture, should we not focus on improving that first?*

#### Summary of relevant Gradle commands
* `gradlew alltests` - run all the tests in this code, including UI and perf
* `gradlew fasttest` - run all the fast tests (unit, integration, API)
* `gradlew uitest` - run only the ui tests
* `gradlew jar` - build the jar, in build/libs/
* `gradlew run` - run the application.  Stop with ctrl+c

> A complex system that works is
> invariably found to have evolved
> from a simple system that
> worked. The inverse proposition
> also appears to be true: A
> complex system designed from
> scratch never works and cannot
> be made to work. You have to
> start over, beginning with a
> working simple system.”
>
> -- _John Gall (Gall's law)_

> If you want to build a ship, don't drum up people to collect wood 
> and don't assign them tasks and work, but rather teach them to long 
> for the endless immensity of the sea. 
> 
> -- _Antoine de Saint-Exupery_

## Screenshots:

<img src="https://github.com/7ep/r3z/blob/master/docs/project_creation.jpg" width="300"> <img src="https://github.com/7ep/r3z/blob/master/docs/login.jpg" width="300"> <img src="https://github.com/7ep/r3z/blob/master/docs/time_entry.jpg" width="300">
<img src="https://github.com/7ep/r3z/blob/master/docs/testEnterTImeRealPerformance.jpg" width="600">
