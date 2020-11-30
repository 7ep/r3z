### r3z

This is an application by [Coveros](https://www.coveros.com/) to demonstrate good
software practices.  As we say in agile... _Working software over comprehensive 
documentation_ ... but that doesn't mean we can't have pretty good documentation too. 

#### Quick Start:

* Put Java on your PATH (see [JDK notes](#java-installation-notes))
* Clone or [download](https://github.com/7ep/r3z/archive/master.zip) this repo. 
 (if you download, unzip the file to a directory.)
* On the command line in the top directory of this repo, 
    * run `gradlew test` to run all the tests (includes UI tests, you need Chrome)
    * run `gradlew run` to run the server
* Visit the application with your browser at http://localhost:8080/



#### Summary:

R3z consists of a web application and tests.  Its goals are: 
* To provide an ecosystem suitable for practicing agile engineering
* To enable research and innovation in valuable development techniques
* To meet business needs in a capabilities-oriented way, rather than being 
tool driven or following the more typical big-design-up-front waterfall methods
* To provide a demonstration of the results of deep agile internally, as well as for clients and partners

*What would happen if you built software in the simplest possible way from scratch?*

*What if our team held quality sacred?*

*What if we spent all the necessary time to think things through?*

*What if we incorporated diverse perspectives?*

*What if testing drove the design?*

*If we understand that our software is a reflection of our culture, should we not focus on improving that first?*

#### Java installation notes

Download the [development kit](https://www.oracle.com/java/technologies/javase-downloads.html#JDK11), 
make note of the installation directory.  Add that directory 
to your path.  For example, on Windows, press the Windows button, type "env" to edit the environment
variables for your account.  Under _user variables_ click New and add:

*  Variable name: JAVA_HOME
*  Variable value: C:\Program Files\Java\jdk-11.0.5   _replace this with the correct path_

Click OK.
Click the PATH user variable and click edit, and then click New, and add a new line for Java
as follows:

    %JAVA_HOME%\bin
    
Test this out by opening a command terminal, for example run the program called "cmd", and run this:
    
    javac -version
    
You should get something similar to the following:

    C:\Users\byron>javac -version
    javac 11.0.5
    
Now you are ready!

#### Summary of relevant Gradle commands
* `gradlew test` - run all the tests in this code
* `gradlew test -Pnoui` - run all the non-ui tests
* `gradlew test -Pjustui` - run only the ui tests
* `gradlew jar` - build the jar, in build/libs/
* `gradlew run` - run the application.  Stop with ctrl+c

> “A complex system that works is
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
> -- “Gall’s law” John Gall

> If you want to build a ship, don't drum up people to collect wood 
> and don't assign them tasks and work, but rather teach them to long 
> for the endless immensity of the sea. 
> 
> -- Antoine de Saint-Exupery
