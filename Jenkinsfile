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
                     sh "ssh admin@63.35.119.99 sudo systemctl start petclinic"
                     sh "sleep 20s"
                     sh "chmod 755 ./curl_assert.sh"
                     sh "./curl_assert.sh > curl_out.json"
                     sh "sleep 10s"
                     script {
                         def output = readJSON file: './curl_out.json'
                         for (int i = 0; i < 5; ++i) {
                             echo output[i].result
                             if (output[i].result == 'TestFail') {
                                 echo "Failure details at https://assertible.com/dashboard#/services/9fa64037-edb3-4758-b441-682ec994b113"
                                 sh "ssh admin@63.35.119.99 sudo systemctl stop petclinic"
                             }
                             assert output[i].result == 'TestPass'
                         }
                     }
                     sh "ssh admin@63.35.119.99 sudo systemctl stop petclinic"
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
                sh "ssh admin@34.245.90.150 sudo systemctl stop petclinic"
                sh "scp target/*.jar admin@34.245.90.150:/home/admin/fromBuildServer/"
                sh "ssh admin@34.245.90.150 sudo systemctl start petclinic"
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
