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
			sh "curl -XPOST https://assertible.com/apis/40a35bd7-dece-4cf4-913d-30f8a718efd5/run?api_token=70yrVBaucYe3KCyD"
			sh "sleep 20s"
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
                    sh "mvn -s $SETTINGS deploy -DskipTests -Dartifactory_url=${env.ARTIFACTORY_URL}"
                }
            }
        }
    }
}
