def labels = ['linux', 'windows']

def builders = [:]

for (x in labels) {
    def label = x
	
    builders[label] = {
		node(label) {
			
			stage('Checkout') {
				checkout scm
			}
			
			stage('Build') {
				sh 'mvn clean install findbugs:findbugs checkstyle:checkstyle jacoco:report'
				junit('**/target/surefire-reports/**/*.xml')
			}
			
			stage('Archive') {
				if (label == 'windows') {
					findbugs('**/target/findbugsXml.xml')
					checkstyle('**/target/checkstyle-result.xml')
					jacoco()
					archiveArtifacts artifacts: '**/target/*.hpi', fingerprint: true
				}
			}
		}
    }
}

parallel builders