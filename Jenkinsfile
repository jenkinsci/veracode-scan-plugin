#!/usr/bin/env groovy
pipeline {
    agent {
        label "maven"
    }
    options {
        timestamps()
    }
    stages {
        stage('Test') {
            steps {
                sh 'mvn -B clean test'
                jacoco()
            }
        }
    }
}