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
			sh "sudo systemctl start petclinic"
<<<<<<< HEAD
			sh "curl -XPOST https://assertible.com/apis/40a35bd7-dece-4cf4-913d-30f8a718efd5/run?api_token=70yrVBaucYe3KCyD"
=======
			sh "curl -XPOST https://assertible.com/apis/40a35bd7-dece-4cf4-913d-30f8a718efd5/run?api_token=70yrVBaucYe3KCyD -d'{
                        "endpoint": "/vets"
                       }"
>>>>>>> 260cf80f45992f558044701b418c01976f44437d
			sh "sudo systemctl stop petclinic"
		}
	}
        stage('Deploy to production') {
            steps {
                sh "ssh admin@52.18.219.125 sudo systemctl stop petclinic2"
                sh "scp target/*.jar admin@52.18.219.125:/home/admin/fromBuildServer/"
                sh "ssh admin@52.18.219.125 sudo systemctl start petclinic2"
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
