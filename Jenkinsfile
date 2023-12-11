pipeline {
    agent any

    environment {
        // 定义环境变量
        JAR_FILE = "my-app-${env.BUILD_NUMBER}.jar"
        BACKUP_DIR = "/path/to/backup/dir"
        SERVER_A = "user@serverA"
        SERVER_B = "user@serverB"
        DEPLOY_PATH = "/path/to/deploy"
    }

    stages {
        stage('从Git更新代码') {
            steps {
                git 'https://your-git-repository-url.git'
            }
        }

        stage('构建Jar包') {
            steps {
                script {
                    sh 'mvn clean package'
                    sh "cp target/${JAR_FILE} ${BACKUP_DIR}/${JAR_FILE}"
                }
            }
        }

        stage('部署到服务器A') {
            steps {
                script {
                    sh "scp ${BACKUP_DIR}/${JAR_FILE} ${SERVER_A}:${DEPLOY_PATH}"
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
                script {
                    sh "scp ${BACKUP_DIR}/${JAR_FILE} ${SERVER_B}:${DEPLOY_PATH}"
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
                // 例如，将上一个版本的Jar包重新部署到服务器A和B
                sh "scp ${BACKUP_DIR}/my-app-$((${BUILD_NUMBER}-1)).jar ${SERVER_A}:${DEPLOY_PATH}"
                sh "ssh ${SERVER_A} 'java -jar ${DEPLOY_PATH}/my-app-$((${BUILD_NUMBER}-1)).jar'"

                sh "scp ${BACKUP_DIR}/my-app-$((${BUILD_NUMBER}-1)).jar ${SERVER_B}:${DEPLOY_PATH}"
                sh "ssh ${SERVER_B} 'java -jar ${DEPLOY_PATH}/my-app-$((${BUILD_NUMBER}-1)).jar'"
            }
        }
    }
}
