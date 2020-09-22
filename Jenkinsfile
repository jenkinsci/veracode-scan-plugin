/* 
The default buildPlugin() call will trigger the parent build script 
from jenkins-infra/pipeline-library and will build on both Linux and Windows nodes.
We are overwriting the default behavior and will only build on Linux node. 
Also JaCoCo, FindBugs and Checkstyle have been added to the build pipeline.
*/

node('linux') {
	
	stage("Checkout") {
		checkout scm
	}
	
	stage("Build") {
		List<String> mavenEnv = [
                    "JAVA_HOME=${tool 'jdk8'}",
                    'PATH+JAVA=${JAVA_HOME}/bin',
                    "PATH+MAVEN=${tool 'mvn'}/bin"]
		withEnv(mavenEnv) {
			sh "mvn clean install findbugs:findbugs checkstyle:checkstyle jacoco:report"
		}
		junit('**/target/surefire-reports/**/*.xml')
	}
	
	stage("Archive") {
		archiveArtifacts artifacts: '**/target/*.hpi', fingerprint: true
	}
}
