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
        stage('Build (Maven, jUnit) and check (SonarQube)') {
            steps {
                parallel(
                        install: {
                            sh "mvn package"
                        },
                        sonar: {
                            withSonarQubeEnv('MySonarQube') {
                                sh "mvn sonar:sonar -Dsonar.host.url=${env.SONARQUBE_HOST}"
                            }
                        }
                )
            }
            post {
                always {
                    junit '**/target/*-reports/TEST-*.xml'
                }
            }
        }

        stage("Quality Gate") {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    // Parameter indicates whether to set pipeline to UNSTABLE if Quality Gate fails
                    // true = set pipeline to UNSTABLE, false = don't
                    // Requires SonarQube Scanner for Jenkins 2.7+
                    waitForQualityGate abortPipeline: true
                }
            }
        }
 
        stage('API tests (Assertible)') {
               steps {
                     sh "ssh admin@52.211.29.193 sudo systemctl start petclinic"
                     sh "sleep 20s"
                     sh "chmod 755 ./curl_assert.sh"
                     sh "./curl_assert.sh > curl_out.json"
                     sh "sleep 10s"
                     script {
                         def output = readJSON file: './curl_out.json'
                         for (int i = 0; i < 5; ++i) {
                             echo output[i].result
                             assert output[i].result == 'TestPass'
                         }
                     }
                     sh "ssh admin@52.211.29.193 sudo systemctl stop petclinic"
               }
        }

        stage('Deploy PetClinic to Artifactory') {
            steps {
                configFileProvider([configFile(fileId: 'our_settings', variable: 'SETTINGS')]) {
                    sh "mvn -s '$SETTINGS' deploy -DskipTests -Dartifactory_url=${env.ARTIFACTORY_URL}"
                }
            }
        }

        stage('Deploy PetClinic to production') {
            steps {
                sh "ssh admin@54.246.219.60 sudo systemctl stop petclinic"
                sh "scp target/*.jar admin@54.246.219.60:/home/admin/fromBuildServer/"
                sh "ssh admin@54.246.219.60 sudo systemctl start petclinic"
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
