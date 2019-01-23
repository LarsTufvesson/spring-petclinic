def callSlack(String buildResult) {
  if ( buildResult == "SUCCESS" ) {
    slackSend color: "good", message: "Job: ${env.JOB_NAME} with buildnumber ${env.BUILD_NUMBER} was successful"
  }
  else if( buildResult == "FAILURE" ) { 
    slackSend color: "danger", message: "Job: ${env.JOB_NAME} with buildnumber ${env.BUILD_NUMBER} was failed"
  }
  else if( buildResult == "UNSTABLE" ) { 
    slackSend color: "warning", message: "Job: ${env.JOB_NAME} with buildnumber ${env.BUILD_NUMBER} was unstable"
  }
  else {
    slackSend color: "danger", message: "Job: ${env.JOB_NAME} with buildnumber ${env.BUILD_NUMBER} its resulat was unclear"	
  }
}

pipeline {
    agent any

    tools {
        jdk 'jdk8'
        maven 'maven3'
    }

    stages {
        stage('Build and check') {
            steps {
                parallel(
                        install: {
                            sh "mvn package"
                        },
                        sonar: {
                            sh "mvn sonar:sonar -Dsonar.host.url=${env.SONARQUBE_HOST}"
                        }
                )
            }
            post {
                always {
                    junit '**/target/*-reports/TEST-*.xml'
                }
            }
        }
	stage('Run API tests'){
		steps {
			sh "ssh admin@34.240.215.249 sudo systemctl start petclinic"
			sh "sleep 20s"
			sh "curl_assert.sh"
			sh "sleep 10s"			
			sh "ssh admin@34.240.215.249 sudo systemctl stop petclinic"
		}
	}
        stage('Deploy to production') {
            steps {
                sh "ssh admin@34.240.250.242 sudo systemctl stop petclinic"
                sh "scp target/*.jar admin@34.240.250.242:/home/admin/fromBuildServer/"
                sh "ssh admin@34.240.250.242 sudo systemctl start petclinic"
            }
        }
        stage('Deploy to Artifactory') {
            steps {
                configFileProvider([configFile(fileId: 'our_settings', variable: 'SETTINGS')]) {
                    sh "mvn -s '$SETTINGS' deploy -DskipTests -Dartifactory_url=${env.ARTIFACTORY_URL}"
                }
            }
        }
    }

    post {
        always {
	    /* Use slackNotifier.groovy from shared library and provide current build result as parameter */   
            callSlack(currentBuild.currentResult)
        }
    }
}
