/**
 * SSH Publisher 方式部署
 *
 * @param action
 *  [Deploy]                        : 发布
 *  [Rollback to last success]      : 回滚到最后一次成功
 *  [Rollback to specified build]   : 回滚到指定到 BUILD_ID
 */
node {
    // 服务名称
    def serverName = JOB_NAME

    // 模块路径: 多个模块在一个仓库时用
    def mavenModule = null

    /**
     * 部署到的机器
     * </p>
     * 系统配置 -> Publish Over SS -> SSH Server -> Name
     */
    def hosts = ['91']

    // Git config
    def branch = 'master'
    def credentialsId = '902b3f40-1482-4120-9688-e3ec133ab1e9'
    def remoteUrl = 'https://code.aliyun.com/sango-server/admin.git'

    // Push resource and exec command
    def sourceDirectory = mavenModule == null ? 'target/' : "${mavenModule}/target/"
    def sourceFiles = "${sourceDirectory}${serverName}.jar"
    def execCommand = "sudo supervisorctl restart ${serverName}"

    def workspace = pwd()

    switch(env.action) {
        case 'Deploy':
            stage('git pull') {
                echo workspace

                // for display purposes
                // Get some code from a GitHub repository
                git branch: branch, credentialsId: credentialsId, url: remoteUrl
            }
            stage('maven build') {
                // Get the Maven tool.
                // ** NOTE: This 'M3' Maven tool must be configured
                // ** in the global configuration.
                mvnHome = tool 'M3'
                sh "${mvnHome}/bin/mvn --version"

                // Run the maven build
                if (mavenModule == null) {
                    sh "${mvnHome}/bin/mvn -B clean package -Dmaven.test.skip=true -Dautoconfig.skip"
                } else {
                    sh "${mvnHome}/bin/mvn -B clean package -pl ${mavenModule} -am -Dmaven.test.skip=true -Dautoconfig.skip"
                }

                archiveArtifacts artifacts: sourceFiles, onlyIfSuccessful: true
            }
            stage('deploy') {
                // 先发布一个节点做验证
                firstHostName = hosts.pop()
                sshPublish(firstHostName, sourceFiles, serverName, execCommand)

                result = input message: '', ok: 'Confirm', parameters: [choice(choices: ['Restore', 'Rollback'], description: '', name: 'action')], submitterParameter: 'operator'
                switch(result.action) {
                // 恢复发布剩余节点
                    case 'Restore':
                        for (hostName in hosts) sshPublish(hostName, sourceFiles, serverName, execCommand)
                        break
                        // 回滚
                    case 'Rollback':
                        currentBuild.result = 'UNSTABLE'

                        artifactPath = getPreSuccessArtifactPath(serverName, sourceFiles)
                        if (artifactPath == null) {
                            echo 'Rollback failed, not available artifact'
                            return
                        }

                        sh "mkdir -p ${sourceDirectory}"
                        sh "cp ${artifactPath} ${sourceDirectory}"

                        sshPublish(firstHostName, sourceFiles, serverName, execCommand)
                        break
                }
            }
            break
        case 'Rollback to last success':
            stage('rollback') {
                artifactPath = getPreSuccessArtifactPath(serverName, sourceFiles)
                if (artifactPath == null) {
                    echo 'Rollback failed, not available artifact'
                    currentBuild.result = 'UNSTABLE'
                    return
                }

                // 必须要复制到工作空间才能上传
                sh "mkdir -p ${sourceDirectory}"
                sh "cp ${artifactPath} ${sourceDirectory}"

                firstHostName = hosts.pop()
                sshPublish(firstHostName, sourceFiles, serverName, execCommand)

                result = input message: '', ok: 'Confirm', parameters: [choice(choices: ['Continue'], description: '', name: 'action')], submitterParameter: 'operator'
                if (result.action == 'Continue') {
                    for (hostName in hosts) sshPublish(hostName, sourceFiles, serverName, execCommand)
                }
            }
            break
        case 'Rollback to specified build':
            stage('rollback') {
                rollbacks = getRollbacks(serverName, sourceFiles)

                if (rollbacks == null || rollbacks.size() == 0) {
                    echo 'There is no version to roll back'
                    currentBuild.result = 'UNSTABLE'
                    return
                }

                result = input message: '', ok: 'Confirm', parameters: [choice(choices: rollbacks, description: '', name: 'version')], submitterParameter: 'operator'

                artifactPath = getArtifactAbsolutePath(serverName, result.version.split('#')[0], sourceFiles)

                sh "mkdir -p ${sourceDirectory}"
                sh "cp ${artifactPath} ${sourceDirectory}"

                firstHostName = hosts.pop()
                sshPublish(firstHostName, sourceFiles, serverName, execCommand)

                result = input message: '', ok: 'Confirm', parameters: [choice(choices: ['Continue'], description: '', name: 'action')], submitterParameter: 'operator'
                if (result.action == 'Continue') {
                    for (hostName in hosts) sshPublish(hostName, sourceFiles, serverName, execCommand)
                }
            }
            break
    }
}

/**
 * 获取上一个可用的制品, 会检测制品文件是否存在
 */
def getPreSuccessArtifactPath(String serverName, String sourceFiles) {
    build = currentBuild.previousSuccessfulBuild;

    while(true) {
        if (build == null) {
            echo "Invalid build"
            return
        }

        artifactPath = getArtifactAbsolutePath(serverName, build.id, sourceFiles)
        if (fileExists(artifactPath) == false) {
            build = build.previousSuccessfulBuild
        } else {
            return artifactPath
        }
    }
}

/**
 * SSH 发布到节点
 */
def sshPublish(String hostName, String sourceFiles, String serverName, String execCommand) {
    removePrefix = sourceFiles.substring(0, sourceFiles.lastIndexOf("/"))
    sshPublisher(
            continueOnError: false,
            failOnError: true,
            publishers: [
                    sshPublisherDesc(
                            configName: hostName,
                            verbose: true,
                            transfers: [
                                    sshTransfer(
                                            sourceFiles: sourceFiles,
                                            removePrefix: removePrefix,
                                            remoteDirectory: serverName,
                                            execCommand: execCommand
                                    )
                            ]
                    )
            ]
    )
}

/**
 * 获取(回滚)可用的制品(版本)列表
 */
def getRollbacks(String serverName, String sourceFiles) {
    list = []

    build = currentBuild.previousSuccessfulBuild;

    while(true) {
        if (build == null) {
            return list
        }

        if (fileExists(getArtifactAbsolutePath(serverName, build.id, sourceFiles)) == true) {
            startTime = new Date(build.startTimeInMillis).format('yy-MM-dd.HH:mm', java.util.TimeZone.getTimeZone('UTC+8'))
            list.add(build.id + '#' + startTime)
        }

        build = build.previousSuccessfulBuild
    }
}

/**
 * 根据 serverName 和 buildId 获取制品路径
 */
def getArtifactAbsolutePath(String serverName, String buildId, String sourceFiles) {
    /*
     * 制品目录结构
     * </p>
     * jenkins_home/jobs/JOB_NAME/builds/build.id/archive/target
     */
    return JENKINS_HOME + '/jobs/' + JOB_NAME + '/builds/' + buildId + '/archive/' + sourceFiles
}