pipeline {
    agent { label 'master' }
    triggers {
        githubPush()
    }
    parameters {
        choice(name: "Enviromnment", choices: ["Developer", "Testing", "main"], description: "Environment deploy (Release=main)") 
        string(name: "AppVersion", defaultValue: "1.0.0", description: "App Version Code (X.Y.Z)")
    }
    stages {
        stage("Init") {
            steps {
                script {
                    comandossh = "ssh -i /var/jenkins_home/keyssh-EC2-prueba.pem ec2-user@JenkinsDockerTF sudo "
                    RepoURL = "https://github.com/jorgon183/ac1cicd/archive/refs/heads/"    //${RepoURL}
                    }
                }
        }                
        stage('CloneRepo') {
            steps {
                echo "-------------------------------------------------------------------"
                sh "${comandossh} ' rm -rf *.zip'"
                sh "${comandossh} ' wget ${RepoURL}${params.Enviromnment}.zip'"
                sh "${comandossh} ' unzip -o ${params.Enviromnment}.zip'"
            }
        }
        stage('DockerBuild') {
            steps {
                echo "-------------------------------------------------------------------"
                sh "${comandossh} ' docker build --build-arg JAR_FILE=products-service-example.jar -t 127.0.0.1:5000/products-service:${params.Enviromnment} ./ac1cicd-${params.Enviromnment} '"
            }
        }   
        stage('DockerRegistry') {
            steps {
                echo "-------------------------------------------------------------------"
                sh "${comandossh} '   docker push 127.0.0.1:5000/products-service:${params.Enviromnment}'"
            }
        }  

        stage('StopDocker') {
            steps {
                echo "-------------------------------------------------------------------"
                sh "${comandossh} 'docker stop products-service-${params.Enviromnment} || true && sudo docker rm products-service-${params.Enviromnment} || true'"
            }
        }
        stage('Deploy') {  
            parallel {
                stage("Developer") {
                    when { expression { params.Enviromnment == "Developer" } }
                    steps {
                        sh "${comandossh} 'docker run -d --name products-service-${params.Enviromnment} -p 80:8080  127.0.0.1:5000/products-service:${params.Enviromnment}'"                
                    }
                }
                stage("Testing") {
                    when { expression { params.Enviromnment == "Testing" } }
                    steps {
                        sh "${comandossh} 'docker run -d --name products-service-${params.Enviromnment} -p 81:8080  127.0.0.1:5000/products-service:${params.Enviromnment}'"                
                    }
                }
                stage("Release") {
                    when { expression { params.Enviromnment == "main" } }
                    steps {
                        sh "${comandossh} 'docker run -d --name products-service-${params.Enviromnment} -p 82:8080  127.0.0.1:5000/products-service:${params.Enviromnment}'"                
                    }
                }
            }           
        }                       
    }
}