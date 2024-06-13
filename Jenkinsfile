// The GIT repository for this pipeline lib is defined in the global Jenkins setting
@Library('jenkins-pipeline-library') import com.gentics.*

// Make the helpers aware of this jobs environment
JobContext.set(this)


final def mavenRepositoryBase  = "https://repo.apa-it.at/artifactory"
final def artifactoryUrlPrefix = mavenRepositoryBase + "/gtx-maven-releases-staging-cms"
final def gitCommitTag         = '[Jenkins | ' + env.JOB_BASE_NAME + ']';

final def testDbManagerHost    = "gcn-testdb-manager.gtx-dev.svc"
final def testDbManagerPort    = "8080"

def branchName                 = null
def version                    = null
def releaseVersion             = ""
def tagName                    = null
def dockerImageTag             = null
def runJUnitTests              = true
def qaDeploy                   = false
def qaDeployBranchList         = ["dev"] as String[]


pipeline {
    agent {
        kubernetes {
            label env.BUILD_TAG.take(63)
            defaultContainer 'build'
            yaml ocpWorker("""
apiVersion: v1
kind: Pod
metadata:
  labels:
    jenkinsbuild: true
spec:
  affinity:
    podAntiAffinity:
      preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 100
        podAffinityTerm:
          labelSelector:
            matchExpressions:
            - key: jenkinsbuild
              operator: In
              values:
              - true
          topologyKey: kubernetes.io/hostname
  containers:
    - name: build
      image: """ + buildEnvironmentDockerImage("build/Dockerfile", "cms") + """
      resources:
        requests:
          cpu: '0'
          memory: '0'
        limits:
          cpu: '0'
          memory: '0'
    - name: docker
      resources:
        limits:
          cpu: '0'
          memory: '0'
        requests:
          cpu: '0'
          memory: '0'
    - name: jnlp
      resources:
        limits:
          cpu: '0'
          memory: '0'
        requests:
          cpu: '0'
          memory: '0'
""")
        }
    }

    parameters {
        booleanParam(name: 'checkGitCommit',            defaultValue: false, description: 'If set to true, the current git revision is compared with the git revision of the last successful build. If they are equal, the build is skipped and env.BUILD_SKIPPED is set to true')
        booleanParam(name: 'runTests',                  defaultValue: true,  description: "Whether to run the unit tests. Tests will be skipped for MR builds if there are no relevant changes.")
        string(name:       'singleTest',                defaultValue: "",    description: "Only this test will be run. Example: com.gentics.contentnode.tests.validation.validator.impl.AttributeValidatorTest")
        booleanParam(name: 'deploy',                    defaultValue: false, description: "Deploy the Maven artifacts, push the docker image and push GIT commits and tags")
        booleanParam(name: 'deployTesting',             defaultValue: false, description: "Like deploy, but only the server image will be built and deployed to a different repository")
        booleanParam(name: 'runReleaseBuild',           defaultValue: false, description: "Do a release build including setting the release version, and adding GIT commits and a GIT tag (last two for releases only)")
        booleanParam(name: 'tagRelease',                defaultValue: true,  description: "Release: Whether to create a GIT tag")
        booleanParam(name: 'releaseWithNewChangesOnly', defaultValue: true,  description: "Release: Abort the build if there are no new changes")
        booleanParam(name: 'mergeHotfixBranch',         defaultValue: true,  description: "Release: Whether to merge the corresponding hotfix branch first (release branches only)")
        booleanParam(name: 'runDockerBuild',            defaultValue: true,  description: "Whether to build the docker image (use deploy to push it also).")
        string(name:       'forceVersion',              defaultValue: "",    description: "If not empty, the build/release will be done using this POM version")
        string(name:       'sourceBranch',              defaultValue: "",    description: "Will only work if the job has */\${sourceBranch} as GIT branch defined")
        string(name:       'cmsOssVersion',             defaultValue: "",    description: "Override CMS OSS version")
    }

    options {
        withCredentials([usernamePassword(credentialsId: 'repo.gentics.com', usernameVariable: 'repoUsername', passwordVariable: 'repoPassword')])
        gitLabConnection('git.gentics.com')
        gitlabBuilds(builds: ['Jenkins build'])
        timestamps()
        timeout(time: 4, unit: 'HOURS')
        ansiColor('xterm')
    }

    triggers {
        gitlab(
            triggerOnPush: false,
            triggerOnMergeRequest: true,
            triggerOpenMergeRequestOnPush: 'source',
            triggerOnNoteRequest: true,
            noteRegex: 'Jenkins please retry a build',
            ciSkip: true,
            skipWorkInProgressMergeRequest: true,
            addNoteOnMergeRequest: true,
            setBuildDescription: true,
            branchFilterType: 'All',
            secretToken: '<redacted>')
    }

    stages {
        stage('Check git commit') {
			when {
				expression {
					return Boolean.valueOf(params.checkGitCommit)
				}
			}

			steps {
				script {
					if ( env.GIT_COMMIT == env.GIT_PREVIOUS_SUCCESSFUL_COMMIT ) {
						echo "env.GIT_COMMIT (" + env.GIT_COMMIT + ") = env.GIT_PREVIOUS_SUCCESSFUL_COMMIT (" + env.GIT_PREVIOUS_SUCCESSFUL_COMMIT + "). Skip building."
						env.BUILD_SKIPPED = "true"
					} else {
                        echo "env.GIT_COMMIT (" + env.GIT_COMMIT + ") != env.GIT_PREVIOUS_SUCCESSFUL_COMMIT (" + env.GIT_PREVIOUS_SUCCESSFUL_COMMIT + "). Need to rebuild."
                    }
 				}
			}
		}

        stage("Build, Deploy") {
            when {
				expression {
					return env.BUILD_SKIPPED != "true"
				}
			}
            steps {
                updateGitlabCommitStatus name: 'Jenkins build', state: "running"

                script {
                    def mvnGoal       = "package"
                    def mvnArguments  = ""

                    version          = params.forceVersion
                    branchName       = GitHelper.fetchCurrentBranchName()

                    if (!version && params.runReleaseBuild) {
                        version = MavenHelper.getVersion()
                    }

                    // Merge the hotfix branch if building a release branch
                    if (params.mergeHotfixBranch && branchName.startsWith("release-")) {
                        def branchToMerge = branchName.replaceFirst(/^release-/, "hotfix-")
                        try {
                            GitHelper.merge('origin/' + branchToMerge)
                        } catch (Exception e) {
                            error 'Couldn\'t merge ref origin/' + branchToMerge + 'into ' + branchName
                        }
                    }

                    if (params.cmsOssVersion?.trim() && !params.runReleaseBuild) {
                        echo "Setting CMS OSS version to " + params.cmsOssVersion.trim()
                        mvnArguments += " -pl '!cms-changelog' -Dcms\\-oss.version=" + params.cmsOssVersion.trim()
                    }

                    if (version) {
                        if (params.runReleaseBuild) {
                            version = MavenHelper.transformSnapshotToReleaseVersion(version)
                        }

                        MavenHelper.setVersion(version)
                        currentBuild.description = version
                    }

                    if (params.runTests) {
                        mvnArguments += (params.singleTest ? " -DfailIfNoTests=false -Dtest=" + params.singleTest : "")
                    } else {
                        mvnArguments           += " -DskipTests=true -Dskip.unit.tests"
                        runJUnitTests = false
                    }

                    // when deploying for the test systems, we only build the server
                    if (params.deployTesting) {
                        mvnArguments += " -am -pl cms-server"
                    }

                    // Update chrome to the latest version
                    //sh "sudo apt-get update"
                    //sh "sudo apt-get install --assume-yes --allow-unauthenticated google-chrome-beta"

                    if (params.runReleaseBuild) {
                        // Release
                        echo "Invoking release build on branch " + branchName + ".."

                        currentBuild.description += ' - Release'

                        if (params.releaseWithNewChangesOnly) {
                            def lastCommitMessage = GitHelper.getLastCommitMessage().trim()

                            if (lastCommitMessage.startsWith(gitCommitTag)) {
                                error "Aborting the release build because there are no new changes. Last commit message is: \"" + lastCommitMessage + "\""
                            }
                        }

                        def pom = readMavenPom file: "pom.xml"
                        def cmsOssVersion = pom.properties['cms-oss.version']
                        echo 'cms-oss.version: ' + cmsOssVersion
                        if (cmsOssVersion.endsWith("-SNAPSHOT")) {
                            error "Can not release with a cms-oss -SNAPSHOT dependency: " + cmsOssVersion
                        }

                        def codeName = null
                        if (branchName =~ /(release|hotfix)-.*/) {
                            def branchNameSplitters = branchName.split('-')
                            codeName = branchNameSplitters[1]
                        } else {
                            echo 'Warning: The current branch name ' + branchName + ' does not match the patterns hotfix-* or release-*. Pushing to the default Artifactory repository'
                        }

                        mvnArguments += (codeName == null ? "" : " -DaltDeploymentRepository=lan.releases.staging.gcn::default::" + artifactoryUrlPrefix)
                        mvnGoal = "deploy"
                    } else {
                        // for now, do not build modules in parallel
                        // mvnArguments += " -T 1C"

                        if (params.deploy || params.deployTesting) {
                            // Deploy
                            mvnGoal = "deploy"
                        }
                    }

                    if (mvnGoal == "deploy") {
                        // Fix for NPE when uploading pom to Artifactory that includes <?m2e ?> directives
                        // See: https://www.jfrog.com/jira/browse/RTFACT-17932
                        sh "find . -maxdepth 3 -type f -name 'pom.xml' -print0 | xargs -0 sed -i -r 's/<\\?m2e[[:blank:]]+[[:alnum:]]+[[:blank:]]*\\?>//g'"
                    }

                    // Add private repository credentials and scopes
                    sh "echo @gentics:registry=https://repo.apa-it.at/api/npm/gtx-npm/ > ~/.npmrc"
                    withCredentials([string(credentialsId: 'artifactory-npm', variable: 'NPM_TOKEN')]) {
                        sh "echo //repo.apa-it.at/api/npm/gtx-npm/:_authToken=${env.NPM_TOKEN} >> ~/.npmrc"
                    }

                    // Login to docker.apa-it.at, so that the tests can pull all Mesh images
                    withDockerRegistry([ credentialsId: "repo.gentics.com", url: "https://docker.apa-it.at/v2" ]) {
                        withDockerRegistry([ credentialsId: "repo.gentics.com", url: "https://gtx-docker-releases-test-system.docker.apa-it.at/v2" ]) {
                            // NX_NON_NATIVE_HASHER temp fix for incompatible installations
                            // see: https://nx.dev/recipes/ci/troubleshoot-nx-install-issues
                            withEnv(["TESTMANAGER_HOSTNAME=" + testDbManagerHost, "TESTMANAGER_PORT=" + testDbManagerPort, "NX_NON_NATIVE_HASHER=true", "TESTCONTAINERS_RYUK_DISABLED=true"]) {
                                sh "mvn -B -Dstyle.color=always -U -Dskip.integration.tests " +
                                    " -fae -Dmaven.test.failure.ignore=true " + mvnArguments + " clean " + mvnGoal
                            }
                        }
                    }

                    if (params.runReleaseBuild) {
                        // Fix for NPE when uploading pom to Artifactory that includes <?m2e ?> directives
                        // See: https://www.jfrog.com/jira/browse/RTFACT-17932
                        // Revert the workaround again for the GIT commit
                        sh "find . -maxdepth 3 -type f -name 'pom.xml' -print0 -exec git checkout {} \\;"
                        MavenHelper.setVersion(version)

                        // Add the modified pom.xml and the generated changelog file
                        def releaseMessage = 'Release of version ' + version
                        GitHelper.addCommit('.', gitCommitTag + ' ' + releaseMessage)

                        if (params.tagRelease) {
                            tagName = version
                            GitHelper.addTag(tagName, releaseMessage)
                        }

                        // It can't hurt to let Artifactory recalculate some meta data
                        for (mavenName in ['cms']) {
                            GenericHelper.recalculateArtifactoryMetadata(mavenRepositoryBase, repoUsername, repoPassword, '/lan.releases/com/gentics/' + mavenName)
                        }
                    }
                }
            }

            post {
                always {
                    script {
                        // Ignore missing test results if we only run one test
                        boolean allowEmptyResults = (params.singleTest ? true : false)

                        if (params.runTests) {
                            if (runJUnitTests) {
                                junit  testResults: "cms-contentstaging/cms-contentstaging-core/target/surefire-reports/TEST-*.xml", allowEmptyResults: allowEmptyResults
                                junit  testResults: "cms-linkchecker/cms-linkchecker-core/target/surefire-reports/TEST-*.xml", allowEmptyResults: allowEmptyResults
                                junit  testResults: "cms-search/cms-search-core/target/surefire-reports/TEST-*.xml", allowEmptyResults: allowEmptyResults
                                junit  testResults: "cms-forms/cms-forms-core/target/surefire-reports/TEST-*.xml", allowEmptyResults: allowEmptyResults
                                junit  testResults: "cms-multichannelling/cms-multichannelling-core/target/surefire-reports/TEST-*.xml", allowEmptyResults: allowEmptyResults
                                junit  testResults: "cms-license/target/surefire-reports/TEST-*.xml", allowEmptyResults: allowEmptyResults
                            }
                        }
                    }
                }
            }
        }

		stage("Docker build") {
			when {
				expression {
					// Build the docker image only if the parameter runDockerBuild is enabled and
					return env.BUILD_SKIPPED != "true" && params.runDockerBuild &&
						(!env.gitlabTargetBranch || qaDeployBranchList.contains(branchName))
				}
			}

            environment {
                DOCKER_TAG   = "${branchName}"
            }

            steps {
                script {
                    def imageHost = "gtx-docker-products.docker.apa-it.at"
                    // if (params.deployTesting) {
                    //     imageHost = "gtx-docker-releases-test-system.docker.apa-it.at"
                    // }
                    def imageName = "${imageHost}/gentics/cms"
                    def imageNameWithTag = "${imageName}:${branchName}"
                    withDockerRegistry([ credentialsId: "repo.gentics.com", url: "https://${imageHost}/v2" ]) {
                        sh "docker --version"
                        sh "cd cms-server ; docker build --network=host -t ${imageNameWithTag} ."

                        // Push released image
                        if (tagName != null) {
                            String dockerImageVersionTag = imageName + ":" + tagName
                            sh "docker tag " + imageNameWithTag + " " + dockerImageVersionTag
                            sh "docker push " + dockerImageVersionTag
                        } else if (params.deploy || params.deployTesting) {
                            // push snapshot build image
                            sh "docker push ${imageNameWithTag}"
                        }
                    }
                }
            }
		}

		stage("Deploy QA images to Kubernetes") {
			when {
				expression {
					return env.BUILD_SKIPPED != "true" && qaDeploy
				}
			}

			steps {
				build job: 'contentnode-qa-deploy',
					parameters: [
						string(name: 'branchName', value: branchName)
					],
					wait: false
			}
		}

		stage("Git push") {
			when {
				expression {
					return env.BUILD_SKIPPED != "true" && params.runReleaseBuild && params.deploy
				}
			}

            steps {
                script {
                    releaseVersion = version
                    version = MavenHelper.getNextSnapShotVersion(version)
                    MavenHelper.setVersion(version)
                    GitHelper.addCommit('.', gitCommitTag + ' Prepare for the next development iteration (' + version + ')')

                    sshagent(["git"]) {
                        GitHelper.pushBranch(branchName)

                        if (tagName != null) {
                            GitHelper.pushTag(tagName)
                        }
                    }
                }
            }
        }
    }

    /* TODO: Re-enable when HTTPS access to gitlab is restored. Due to infrastructure issues this is disabled temporarily.
    post {
        always {
            updateGitlabCommitCurrentBuildStatus name: 'Jenkins build'
        }
    }
    */
}
