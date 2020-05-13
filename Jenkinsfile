node('linux') {
	
	jdk = tool name: 'JDK8'
  	env.JAVA_HOME = "${jdk}"
	sh '$JAVA_HOME/bin/java -version'
	
	stage("Checkout") {
		checkout scm
	}
	
	stage("Build") {
		sh 'mvn clean install findbugs:findbugs checkstyle:checkstyle jacoco:report'
		junit('**/target/surefire-reports/**/*.xml')
	}
	
	stage("Archive") {
		findbugs('**/target/findbugsXml.xml')
		checkstyle('**/target/checkstyle-result.xml')
		jacoco()
		archiveArtifacts artifacts: '**/target/*.hpi', fingerprint: true
	}
}
