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
				String command = "mvn clean install findbugs:findbugs checkstyle:checkstyle jacoco:report"
				if (label == 'linux') {
					sh command
				} else {
					bat command
				}
				junit('**/target/surefire-reports/**/*.xml')
			}
			
			stage('Archive') {
				if (isLinux(label)) {
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

boolean isLinux(String label) {
    label == 'linux'
}