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
			options { timeout(time: 30, unit: 'MINUTES') }
			environment {
				ARTIFACTORY = credentials("${p['artifactory.credentials']}")
			}
			steps {
				script {
					docker.withRegistry(p['docker.registry'], p['docker.credentials']) {
						docker.image(p['docker.java.main.image']).inside(p['docker.java.inside.basic']) {
							sh 'rm -Rf `find . -name "BACKUPDEFAULT*"`'
							sh 'rm -Rf `find . -name "ConfigDiskDir*"`'
							sh 'rm -Rf `find . -name "locator*" | grep -v "src"`'
							sh 'rm -Rf `find . -name "newDB"`'
							sh 'rm -Rf `find . -name "server" | grep -v "src"`'
							sh 'rm -Rf `find . -name "*.log"`'
							sh 'MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home -Duser.dir=$PWD -Djava.io.tmpdir=/tmp" ./mvnw -s settings.xml clean dependency:list test -Dsort -U -B'
						}
					}
				}
			}
		}

		stage("Test other configurations") {
			when {
				beforeAgent(true)
				allOf {
					branch(pattern: "main|(\\d\\.\\d\\.x)", comparator: "REGEXP")
					not { triggeredBy 'UpstreamCause' }
				}
			}
			parallel {
				stage("test: baseline (next)") {
					agent {
						label 'data'
					}
					options { timeout(time: 30, unit: 'MINUTES') }
					environment {
						ARTIFACTORY = credentials("${p['artifactory.credentials']}")
					}
					steps {
						script {
							docker.withRegistry(p['docker.registry'], p['docker.credentials']) {
								docker.image(p['docker.java.next.image']).inside(p['docker.java.inside.basic']) {
									sh 'rm -Rf `find . -name "BACKUPDEFAULT*"`'
									sh 'rm -Rf `find . -name "ConfigDiskDir*"`'
									sh 'rm -Rf `find . -name "locator*" | grep -v "src"`'
									sh 'rm -Rf `find . -name "newDB"`'
									sh 'rm -Rf `find . -name "server" | grep -v "src"`'
									sh 'rm -Rf `find . -name "*.log"`'
									sh 'MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home -Duser.dir=$PWD -Djava.io.tmpdir=/tmp" ./mvnw -s settings.xml clean dependency:list test -Dsort -U -B'
								}
							}
						}
					}
				}

				stage("test: baseline (LTS)") {
					agent {
						label 'data'
					}
					options { timeout(time: 30, unit: 'MINUTES') }
					environment {
						ARTIFACTORY = credentials("${p['artifactory.credentials']}")
					}
					steps {
						script {
							docker.withRegistry(p['docker.registry'], p['docker.credentials']) {
								docker.image(p['docker.java.lts.image']).inside(p['docker.java.inside.basic']) {
									sh 'rm -Rf `find . -name "BACKUPDEFAULT*"`'
									sh 'rm -Rf `find . -name "ConfigDiskDir*"`'
									sh 'rm -Rf `find . -name "locator*" | grep -v "src"`'
									sh 'rm -Rf `find . -name "newDB"`'
									sh 'rm -Rf `find . -name "server" | grep -v "src"`'
									sh 'rm -Rf `find . -name "*.log"`'
									sh 'MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home -Duser.dir=$PWD -Djava.io.tmpdir=/tmp" ./mvnw -s settings.xml -P remote-java17 clean dependency:list test -Dsort -U -B'
								}
							}
						}
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
					docker.withRegistry(p['docker.registry'], p['docker.credentials']) {
						docker.image(p['docker.java.main.image']).inside(p['docker.java.inside.basic']) {
							sh 'rm -Rf `find . -name "BACKUPDEFAULT*"`'
							sh 'rm -Rf `find . -name "ConfigDiskDir*"`'
							sh 'rm -Rf `find . -name "locator*" | grep -v "src"`'
							sh 'rm -Rf `find . -name "newDB"`'
							sh 'rm -Rf `find . -name "server" | grep -v "src"`'
							sh 'rm -Rf `find . -name "*.log"`'
							sh 'MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home -Duser.dir=$PWD -Djava.io.tmpdir=/tmp	" ./mvnw -s settings.xml -Pci,artifactory ' +
									'-Dartifactory.server=https://repo.spring.io ' +
									"-Dartifactory.username=${ARTIFACTORY_USR} " +
									"-Dartifactory.password=${ARTIFACTORY_PSW} " +
									"-Dartifactory.staging-repository=libs-snapshot-local " +
									"-Dartifactory.build-name=spring-data-geode " +
									"-Dartifactory.build-number=${BUILD_NUMBER} " +
									'-Dmaven.test.skip=true clean deploy -U -B'
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
