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
				List<String> env = [
		            "JAVA_HOME=${tool jdk8}",
		            'PATH+JAVA=${JAVA_HOME}/bin',
		        ]
				
				withEnv(env) {
					String command = "mvn clean install findbugs:findbugs checkstyle:checkstyle jacoco:report"
					if (label == 'linux') {
						sh command
					} else {
						bat command
					}
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