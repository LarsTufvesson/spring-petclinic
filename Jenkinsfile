@Grapes(
    @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1')
)
import static groovyx.net.http.ContentType.TEXT


def checkSonar() {
  // Define URL variable
  String taskURL = "http://34.240.215.249:9000/api/ce/task?id=TASK_ID"
  String projectStatusURL = "http://34.240.215.249:9000/api/qualitygates/project_status?analysisId="

  // Get project status
  def status=taskURL(taskURL).task.status
  while ( status == "PENDING" || status == "IN_PROGRESS" ) {
    println "waiting for sonar results"
    status = httpClient(taskURL).task.status
    sleep(1000)
  }
      assert status != "CANCELED" : "Build fail because sonar project is CANCELED"
      assert status != "FAILED" : "Build fail because sonar project is FAILED"
      def qualitygates= httpClient(projectStatusURL + httpClient(taskURL).task.analysisId)
      assert qualitygates.projectStatus.status != "ERROR" : "Build fail because sonar project status is not ok"
      println "Huraaaah! You made it :) Sonar Results are good"
}

def httpClient(String url){
    def taskClient = new groovyx.net.http.HTTPBuilder(url)
    taskClient.setHeaders(Accept: 'application/json')
    def response =  taskClient.get(contentType: TEXT)
    def sluper = new groovy.json.JsonSlurper().parse(response)
}      

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
        stage('Build (Maven, jUnit) and check (Sonar)') {
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
	stage('API tests (Assertible)'){
		steps {
			sh "ssh admin@34.240.215.249 sudo systemctl start petclinic"
			sh "sleep 20s"
			sh "chmod 755 ./curl_assert.sh"
			sh "./curl_assert.sh"
			sh "sleep 10s"			
			sh "ssh admin@34.240.215.249 sudo systemctl stop petclinic"
		}
	}
        stage('Deploy PetClinic to staging/production') {
            steps {
                sh "ssh admin@34.240.250.242 sudo systemctl stop petclinic"
                sh "scp target/*.jar admin@34.240.250.242:/home/admin/fromBuildServer/"
                sh "ssh admin@34.240.250.242 sudo systemctl start petclinic"
            }
        }
/* 
        stage('Deploy PetClinic Artifactory') {
            steps {
                configFileProvider([configFile(fileId: 'our_settings', variable: 'SETTINGS')]) {
                    sh "mvn -s '$SETTINGS' deploy -DskipTests -Dartifactory_url=${env.ARTIFACTORY_URL}"
                }
            }
        }
*/
    }

    post {
        always {
	    /* Use slackNotifier.groovy from shared library and provide current build result as parameter */   
            callSlack(currentBuild.currentResult)
        }
    }
}
