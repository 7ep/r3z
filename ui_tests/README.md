This project exists solely to enable UI testing on the affiliated project

#### Chromedriver installation notes
Make sure that the [Chromedriver](https://chromedriver.chromium.org/) executable is installed in one of the directories that is 
on your path.  To see your path, type the following in a command line: 

on Windows:

    echo %PATH%  
    
On Mac/Linux:

    echo $PATH
    
If you run the command, `chromedriver` on the command  line, you should get a result similar to this:

    Starting ChromeDriver ...
    
#### Summary of relevant Gradle commands
* gradlew test - run all the UI tests in this code. Note: it is necessary that the program under
test is running before running these tests.    