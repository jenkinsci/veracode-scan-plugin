# Veracode Scan Plugin for Jenkins
[![Contributors](https://img.shields.io/github/contributors/jenkinsci/veracode-scan-plugin.svg)](https://github.com/jenkinsci/veracode-scan-plugin/graphs/contributors)
[![Build Status](https://ci.jenkins.io/job/Plugins/job/veracode-scan-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/veracode-scan-plugin/job/master/)

## General
Veracode has productised a plugin to make it simple for you to submit to Veracode applications already configured in Jenkins.
For more information, please visit the [Veracode Jenkins Plugin documentation](https://help.veracode.com/reader/PgbNZUD7j8aY7iG~hQZWxQ/yQtYXnlbLA6wsWodLn5zdw)

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