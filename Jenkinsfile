pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '30'))
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
        timestamps()
    }

    parameters {
        booleanParam(name: 'PUBLISH_IMAGES', defaultValue: false,
            description: 'Publish commit-tagged images. Allowed only for main.')
        booleanParam(name: 'DEPLOY_TO_PRODUCTION', defaultValue: false,
            description: 'Deploy an already published tag after manual approval. Allowed only for main.')
        string(name: 'IMAGE_REPOSITORY', defaultValue: '',
            description: 'Registry path without a trailing slash, for example registry.example.com/story.')
        string(name: 'DEPLOY_DOCKER_HOST', defaultValue: '',
            description: 'Docker SSH endpoint, for example ssh://deploy@example.com.')
        string(name: 'DEPLOY_IMAGE_TAG', defaultValue: '',
            description: 'Optional existing tag to deploy or roll back to. Empty means this commit SHA.')
    }

    environment {
        CI_IMAGE_REPOSITORY = 'story-project'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.IMAGE_TAG = sh(
                        script: 'git rev-parse --short=12 HEAD',
                        returnStdout: true
                    ).trim()
                    currentBuild.displayName = "#${env.BUILD_NUMBER} ${env.IMAGE_TAG}"
                }
            }
        }

        stage('Backend verify') {
            steps {
                sh '''
                    mkdir -p .ci-cache/m2
                    docker run --rm \
                      --user "$(id -u):$(id -g)" \
                      -e HOME=/tmp \
                      -v "$PWD:/workspace" \
                      -v "$PWD/.ci-cache/m2:/tmp/.m2" \
                      -w /workspace \
                      maven:3.9-eclipse-temurin-21 \
                      mvn -B -ntp -Dmaven.repo.local=/tmp/.m2 clean verify
                '''
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Frontend verify') {
            steps {
                sh '''
                    mkdir -p .ci-cache/npm
                    docker run --rm \
                      --user "$(id -u):$(id -g)" \
                      -e HOME=/tmp \
                      -e CI=true \
                      -v "$PWD:/workspace" \
                      -w /workspace/Story-Front \
                      node:20-alpine sh -lc \
                      'npm ci --cache /workspace/.ci-cache/npm --no-audit --no-fund && npm test -- --watchAll=false --passWithNoTests && npm run build'
                '''
            }
        }

        stage('Build commit images') {
            steps {
                sh 'chmod +x ci/build-images.sh && ci/build-images.sh "$CI_IMAGE_REPOSITORY" "$IMAGE_TAG"'
            }
        }

        stage('Publish images') {
            when {
                allOf {
                    branch 'main'
                    expression { params.PUBLISH_IMAGES }
                }
            }
            steps {
                script {
                    if (!params.IMAGE_REPOSITORY?.trim()) {
                        error('IMAGE_REPOSITORY is required when PUBLISH_IMAGES is enabled')
                    }
                }
                withCredentials([usernamePassword(
                    credentialsId: 'story-registry',
                    usernameVariable: 'REGISTRY_USERNAME',
                    passwordVariable: 'REGISTRY_PASSWORD'
                )]) {
                    sh '''
                        registry_host="${IMAGE_REPOSITORY%%/*}"
                        printf '%s' "$REGISTRY_PASSWORD" | docker login "$registry_host" \
                          --username "$REGISTRY_USERNAME" --password-stdin
                        ci/build-images.sh "$IMAGE_REPOSITORY" "$IMAGE_TAG" --push-from "$CI_IMAGE_REPOSITORY"
                        docker logout "$registry_host"
                    '''
                }
            }
        }

        stage('Production approval') {
            when {
                allOf {
                    branch 'main'
                    expression { params.DEPLOY_TO_PRODUCTION }
                }
            }
            input {
                message 'Deploy the selected immutable image tag to production?'
                ok 'Deploy'
                submitterParameter 'APPROVED_BY'
            }
            steps {
                echo 'Production deployment approved.'
            }
        }

        stage('Production deploy') {
            when {
                allOf {
                    branch 'main'
                    expression { params.DEPLOY_TO_PRODUCTION }
                }
            }
            steps {
                script {
                    if (!params.IMAGE_REPOSITORY?.trim() || !params.DEPLOY_DOCKER_HOST?.trim()) {
                        error('IMAGE_REPOSITORY and DEPLOY_DOCKER_HOST are required for deployment')
                    }
                    env.SELECTED_DEPLOY_TAG = params.DEPLOY_IMAGE_TAG?.trim() ?: env.IMAGE_TAG
                }
                withCredentials([file(credentialsId: 'story-production-env', variable: 'STORY_PRODUCTION_ENV')]) {
                    sshagent(credentials: ['story-production-ssh']) {
                        sh '''
                            chmod +x ci/deploy-compose.sh
                            ci/deploy-compose.sh \
                              "$IMAGE_REPOSITORY" \
                              "$SELECTED_DEPLOY_TAG" \
                              "$DEPLOY_DOCKER_HOST" \
                              "$STORY_PRODUCTION_ENV"
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts allowEmptyArchive: true,
                artifacts: '**/target/surefire-reports/**,Story-Front/build/**',
                fingerprint: true
            cleanWs(deleteDirs: true, disableDeferredWipeout: true)
        }
    }
}
