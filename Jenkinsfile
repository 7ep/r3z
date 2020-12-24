pipeline {

  agent any

  stages {

    // build the war file (the binary).  This is the only
    // place that happens.
    stage('Build') {
      steps {
        sh './gradlew clean test jar'
      }
      post {
        always {
          junit 'build/test-results/test/*.xml'
        }
      }
    }

    // Runs an analysis of the code, looking for any
    // patterns that suggest potential bugs.
    stage('Static Analysis') {
      steps {
        sh './gradlew sonarqube'
        // wait for sonarqube to finish its analysis
        sleep 5
        sh './gradlew checkQualityGate'
      }
    }


    // Run OWASP's "DependencyCheck". https://owasp.org/www-project-dependency-check/
    // You are what you eat - and so it is with software.  This
    // software consists of a number of software by other authors.
    // For example, for this project we use language tools by Apache,
    // password complexity analysis, and several others.  Each one of
    // these might have security bugs - and if they have a security
    // bug, so do we!
    //
    // DependencyCheck looks at the list of known
    // security vulnerabilities from the United States National Institute of
    // Standards and Technology (NIST), and checks if the software
    // we are importing has any major known vulnerabilities. If so,
    // the build will halt at this point.
    stage('Security: Dependency Analysis') {
      steps {
         sh './gradlew dependencyCheckAnalyze'
      }
    }

    stage('Build Documentation') {
      steps {
         sh './gradlew dokka'
      }
    }

    // This is the stage where we deploy to production.  If any test
    // fails, we won't get here.  Note that we aren't really doing anything - this
    // is a token step, to indicate whether we would have deployed or not.  Nothing actually
    // happens, since this is a demo project.
    stage('Deploy to Prod') {
      steps {
        // deploy to prod
        sh 'scp build/libs/r3z.jar byron@renomad.com:~/r3z && ssh byron@renomad.com "~/r3z/stop.sh && sleep 2 && ~/r3z/start.sh"'
      }
    }

  }

}
