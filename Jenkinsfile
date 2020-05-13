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
		findbugs(pattern: '**/target/findbugsXml.xml')
		checkstyle(pattern: '**/target/checkstyle-result.xml')
		jacoco()
		archiveArtifacts artifacts: '**/target/*.hpi', fingerprint: true
	}
}
