node('linux') {
	
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
