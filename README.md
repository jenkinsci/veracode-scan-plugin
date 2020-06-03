# Veracode for Jenkins
[![Contributors](https://img.shields.io/github/contributors/jenkinsci/veracode-scan-plugin.svg)](https://github.com/jenkinsci/veracode-scan-plugin/graphs/contributors)
[![Build Status](https://ci.jenkins.io/job/Plugins/job/veracode-scan-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/veracode-scan-plugin/job/master/)
[![Jenkins Version](https://img.shields.io/badge/Jenkins-2.60-green.svg?label=min.%20Jenkins)](https://jenkins.io/download/)
![JDK8](https://img.shields.io/badge/jdk-8-yellow.svg?label=min.%20JDK)
[![License: MIT](https://img.shields.io/badge/license-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## General
Veracode partners with companies that innovate through software to confidently deliver secure code on time. Veracode addresses common Application Security challenges with a unique combination of automated application analysis in the pipeline, plus DevSecOps expertise for developers and security professionals, all delivered through a scalable SaaS platform.

Veracode for Jenkins is a plugin that automates the submission of applications to Veracode for scanning, packaging it in Veracode's preferred format. Veracode for Jenkins contributes a "Post-Build" action that can be used to configure jobs to scan your own source code (SAST) or open source libraries (SCA) as well as testing running applications with dynamic analysis (DAST) or interactive application security testing (IAST).  

For more info and resources, please visit the [Veracode Community](https://community.veracode.com/s/knowledgeitem/jenkins-20Y2T000000KypdUAC). 

## How to Contribute
Veracode welcomes community contribution through pull requests.

### Important:
The plugin code is stored in github repositories:
https://github.com/jenkinsci/veracode-scan-plugin

Please make sure to submit pull requests to above repository

## How to build the plugin code
To build the plugin, please use Maven 3.3.9 or above, with JDK 8, and run:
```console
> mvn clean package
```
