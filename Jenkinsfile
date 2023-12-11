pipeline {
    agent any

    environment {
        // 定义环境变量
        JAR_FILE = "${env.BUILD_NUMBER}.jar"
        BACKUP_DIR = "${WORKSPACE}/bak"
        SERVER_A = "ec2-user@18.140.62.185"
        SERVER_B = "ec2-user@18.140.62.185"
        DEPLOY_PATH = "/home/project/usa-test"
        SSH_CREDENTIAL_ID = '902b3f40-1482-4120-9688-e3ec133ab1e9'
    }

    stages {
        stage('从Git更新代码') {
            steps {
                git 'https://gitee.com/YueGod/test-service.git'
            }
        }

        stage('构建Jar包') {
            steps {
                script {
                    sh 'mvn clean package'
                }
            }
        }

        stage('备份Jar包') {
            steps {
                script {
                    sh """
                        mkdir -p ${BACKUP_DIR}/${BUILD_NUMBER}
                        cp -f ${WORKSPACE}/target/*.jar ${BACKUP_DIR}/${BUILD_NUMBER}/
                    """
                }
            }
        }

        stage('部署到服务器A') {
                    steps {
                        sshagent([SSH_CREDENTIAL_ID]) {
                            sh "scp ${BACKUP_DIR}/${BUILD_NUMBER}/${JAR_FILE} ${SERVER_A}:${DEPLOY_PATH}"
                            sh "ssh ${SERVER_A} 'java -jar ${DEPLOY_PATH}/${JAR_FILE}'"
                        }
                    }
                }



        stage('人工审核') {
            steps {
                input '确认部署到服务器A是否成功？'
            }
        }

        stage('部署到服务器B') {
                            steps {
                                sshagent([SSH_CREDENTIAL_ID]) {
                                    sh "scp ${BACKUP_DIR}/${BUILD_NUMBER}/${JAR_FILE} ${SERVER_B}:${DEPLOY_PATH}"
                                    sh "ssh ${SERVER_B} 'java -jar ${DEPLOY_PATH}/${JAR_FILE}'"
                                }
                            }
                        }
    }

    post {
        failure {
            script {
                // 回滚逻辑
                // 这里需要根据实际情况编写回滚脚本
                // 例如，将指定版本的Jar包重新部署到服务器A和B
                // 需要在Jenkins中设置参数Version
                sh """
                    cp -f ${BACKUP_DIR}/${Version}/*.jar ${WORKSPACE/target/
                    scp ${WORKSPACE}/target/*.jar ${SERVER_A}:${DEPLOY_PATH}
                    ssh ${SERVER_A} 'java -jar ${DEPLOY_PATH}/*.jar'
                    scp ${WORKSPACE}/target/*.jar ${SERVER_B}:${DEPLOY_PATH}
                    ssh ${SERVER_B} 'java -jar ${DEPLOY_PATH}/*.jar'
                """
            }
        }
    }
}
