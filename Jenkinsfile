def p = [:]

node {
    checkout scm
    p = readProperties interpolate: true, file: 'ci/pipeline.properties'
}

pipeline {

	agent none

	triggers {
		pollSCM 'H/10 * * * *'
		upstream(upstreamProjects: "spring-data-commons/main", threshold: hudson.model.Result.SUCCESS)
	}

	options {
		disableConcurrentBuilds()
		buildDiscarder(logRotator(numToKeepStr: '14'))
	}

	stages {
		stage("test: baseline (main)") {
			when {
				beforeAgent(true)
				anyOf {
					branch(pattern: "main|(\\d\\.\\d\\.x)", comparator: "REGEXP")
					not { triggeredBy 'UpstreamCause' }
				}
			}
			agent {
				label 'data'
			}
			options { timeout(time: 60, unit: 'MINUTES') }
			environment {
				ARTIFACTORY = credentials("${p['artifactory.credentials']}")
			}
			steps {
				script {
					docker.image(p['docker.java.main.image']).inside(p['docker.java.inside.basic']) {
						sh 'rm -Rf `find . -name "BACKUPDEFAULT*"`'
						sh 'rm -Rf `find . -name "ConfigDiskDir*"`'
						sh 'rm -Rf `find . -name "locator*" | grep -v "src"`'
						sh 'rm -Rf `find . -name "newDB"`'
						sh 'rm -Rf `find . -name "server" | grep -v "src"`'
						sh 'rm -Rf `find . -name "*.log"`'
						sh 'GRADLE_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home -Duser.dir=$PWD -Djava.io.tmpdir=/tmp" ./gradlew clean check --no-daemon --refresh-dependencies --stacktrace'
					}
				}
			}
		}

		stage('Release to artifactory') {
			when {
				beforeAgent(true)
				anyOf {
					branch(pattern: "main|(\\d\\.\\d\\.x)", comparator: "REGEXP")
					not { triggeredBy 'UpstreamCause' }
				}
			}
			agent {
				label 'data'
			}
			options { timeout(time: 20, unit: 'MINUTES') }

			environment {
				ARTIFACTORY = credentials("${p['artifactory.credentials']}")
			}

			steps {
				script {
					docker.image(p['docker.container.image.java.main']).inside(p['docker.container.inside.env.basic']) {
						withCredentials([file(credentialsId: 'spring-signing-secring.gpg', variable: 'SIGNING_KEYRING_FILE')]) {
							withCredentials([string(credentialsId: 'spring-gpg-passphrase', variable: 'SIGNING_PASSWORD')]) {
								withCredentials([usernamePassword(credentialsId: 'oss-token', usernameVariable: 'OSSRH_USERNAME', passwordVariable: 'OSSRH_PASSWORD')]) {
									withCredentials([usernamePassword(credentialsId: p['artifactory.credentials'], usernameVariable: 'ARTIFACTORY_USERNAME', passwordVariable: 'ARTIFACTORY_PASSWORD')]) {
										sh 'rm -Rf `find . -name "BACKUPDEFAULT*"`'
										sh 'rm -Rf `find . -name "ConfigDiskDir*"`'
										sh 'rm -Rf `find . -name "locator*" | grep -v "src"`'
										sh 'rm -Rf `find . -name "newDB"`'
										sh 'rm -Rf `find . -name "server" | grep -v "src"`'
										sh 'rm -Rf `find . -name "*.log"`'
										sh 'GRADLE_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home -Duser.dir=$PWD -Djava.io.tmpdir=/tmp" ./gradlew publishArtifacts releasePublishedArtifacts --no-build-cache --no-configuration-cache --no-daemon --stacktrace ' +
											"-PartifactoryUsername=${ARTIFACTORY_USERNAME} " +
											"-PartifactoryPassword=${ARTIFACTORY_PASSWORD} " +
											"-PossrhUsername=${OSSRH_USERNAME} " +
											"-PossrhPassword=${OSSRH_PASSWORD} " +
											"-Psigning.keyId=${SPRING_SIGNING_KEYID} " +
											"-Psigning.password=${SIGNING_PASSWORD} " +
											"-Psigning.secretKeyRingFile=${SIGNING_KEYRING_FILE} " +
											'-x test - x integrationTest'
									}
								}
							}
						}
					}
				}
			}
		}
	}

	post {
		changed {
			script {
				slackSend(
						color: (currentBuild.currentResult == 'SUCCESS') ? 'good' : 'danger',
						channel: '#spring-data-dev',
						message: "${currentBuild.fullDisplayName} - `${currentBuild.currentResult}`\n${env.BUILD_URL}")
				emailext(
						subject: "[${currentBuild.fullDisplayName}] ${currentBuild.currentResult}",
						mimeType: 'text/html',
						recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']],
						body: "<a href=\"${env.BUILD_URL}\">${currentBuild.fullDisplayName} is reported as ${currentBuild.currentResult}</a>")
			}
		}
	}
}
