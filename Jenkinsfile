pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                buildPlugin(findbugs: [archive: true])
            }
        }
    }
}