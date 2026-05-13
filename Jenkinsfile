// The GIT repository for this pipeline lib is defined in the global Jenkins setting
@Library('jenkins-pipeline-library') import com.gentics.*

// Make the helpers aware of this jobs environment
JobContext.set(this)


final def gitCommitTag         = '[Jenkins | ' + env.JOB_BASE_NAME + ']';

final def testDbManagerHost    = "gcn-testdb-manager.gtx-dev.svc"
final def testDbManagerPort    = "8080"
final def helmRepositoryProd   = 'https://repo.gentics.com/repository/helm/';
final def helmRepositoryTest   = 'https://repo.gentics.com/repository/helm-snapshots/';

def branchName                 = null
def version                    = null
def helmVersion                = null
def releaseVersion             = ""
def tagName                    = null
def dockerImageTag             = null
def runJUnitTests              = true
def qaDeploy                   = false
def qaDeployBranchList         = ["dev"] as String[]

def imageHost                  = "push.docker.gentics.com"
def imageRepo                  = "docker-snapshots"
def helmRepository             = helmRepositoryProd;

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
      image: docker.gentics.com/cms-oss/build-container:6.4
      resources:
        requests:
          cpu: '0'
          memory: '0'
        limits:
          cpu: '0'
          memory: '0'
    - name: docker
      image: docker:29.0.4-dind-rootless
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
        booleanParam(name: 'integrationTests',          defaultValue: false,  description: "Whether to run integration tests.")
        booleanParam(name: 'deploy',                    defaultValue: false, description: "Deploy the Maven artifacts, push the docker image and push GIT commits and tags")
        booleanParam(name: 'deployTesting',             defaultValue: false, description: "Like deploy, but only the server image will be built and deployed to a different repository")
        booleanParam(name: 'runReleaseBuild',           defaultValue: false, description: "Do a release build including setting the release version, and adding GIT commits and a GIT tag (last two for releases only)")
        booleanParam(name: 'tagRelease',                defaultValue: true,  description: "Release: Whether to create a GIT tag")
        booleanParam(name: 'releaseWithNewChangesOnly', defaultValue: true,  description: "Release: Abort the build if there are no new changes")
        booleanParam(name: 'mergeHotfixBranch',         defaultValue: true,  description: "Release: Whether to merge the corresponding hotfix branch first (release branches only)")
        booleanParam(name: 'runDockerBuild',            defaultValue: true,  description: "Whether to build the docker image (use deploy to push it also).")
        booleanParam(name: 'omitScan',                  defaultValue: false, description: "Omit scanning the docker image")
        booleanParam(name: 'runHelmBuild',              defaultValue: false, description: "Whether to run the helm build steps.")
        string(name:       'forceVersion',              defaultValue: "",    description: "If not empty, the build/release will be done using this POM version")
        string(name:       'sourceBranch',              defaultValue: "",    description: "Will only work if the job has */\${sourceBranch} as GIT branch defined")
        string(name:       'cmsOssVersion',             defaultValue: "",    description: "Override CMS OSS version")
        string(name:       'meshPluginsVersion',        defaultValue: "",    description: "Optional version of mesh plugins (for testing)")
    }

    options {
        withCredentials([usernamePassword(credentialsId: 'docker.gentics.com', usernameVariable: 'repoUsername', passwordVariable: 'repoPassword')])
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
                    def mvnProjects   = ""
                    def mvnArguments  = ""

                    version          = params.forceVersion
                    branchName       = GitHelper.fetchCurrentBranchName()

                    if (!version && params.runReleaseBuild) {
                        version = MavenHelper.getVersion()
                    }
                    helmVersion = version

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
                        MavenHelper.setProperty("cms-oss.version", params.cmsOssVersion.trim())
                        mvnProjects = " -am -pl '!cms-changelog,!cms-doc'"
                    }

                    if (params.meshPluginsVersion?.trim() && !params.runReleaseBuild) {
                        echo "Setting Mesh Plugins version to " + params.meshPluginsVersion.trim()
                        MavenHelper.setProperty("mesh.forms.plugin.version", params.meshPluginsVersion.trim())
                    }

                    if (version) {
                        if (params.runReleaseBuild) {
                            version = MavenHelper.transformSnapshotToReleaseVersion(version)
                            helmVersion = version
                        }

                        MavenHelper.setVersion(version, mvnArguments)
                        currentBuild.description = version
                    }

                    if (params.runTests) {
                        mvnArguments += (params.singleTest ? " -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=" + params.singleTest : "")
                        if (!params.runReleaseBuild && !params.deploy && !params.deployTesting) {
                            mvnProjects = " -am -pl cms-server"
                        }
                    } else {
                        mvnArguments           += " -DskipTests=true -Dskip.unit.tests"
                        runJUnitTests = false
                    }

                    // when deploying for the test systems, we only build the server
                    if (params.deployTesting) {
                        mvnProjects = " -am -pl cms-server,formgen"
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
                    sh "echo @gentics:registry=https://repo.gentics.com/repository/npm-products/> ~/.npmrc"
                    withCredentials([string(credentialsId: 'nexus-npm', variable: 'NPM_TOKEN')]) {
                        sh "echo //repo.gentics.com/repository/npm-products/:_auth=${env.NPM_TOKEN} >> ~/.npmrc"
                    }

                    // Login to docker.gentics.com, so that the tests can pull all Mesh images
                    authDockerRegistry("docker.gentics.com", "docker.gentics.com")
                    authDockerRegistry("docker.gentics.com", "push.docker.gentics.com")
                    // NX_NON_NATIVE_HASHER temp fix for incompatible installations
                    // see: https://nx.dev/recipes/ci/troubleshoot-nx-install-issues
                    withEnv(["TESTMANAGER_HOSTNAME=" + testDbManagerHost, "TESTMANAGER_PORT=" + testDbManagerPort, "NX_NON_NATIVE_HASHER=true", "TESTCONTAINERS_RYUK_DISABLED=true", "LICENSEKEY=${mesh_enterprise_licensekey_2}", "KEYS_PATH=target/keys"]) {
                        sh "mvn -B -Dstyle.color=always -U -Dskip.integration.tests " +
                            " -fae -Dmaven.test.failure.ignore=true " + mvnArguments + mvnProjects + " clean " + mvnGoal
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
                                junit  testResults: "cms-translation/cms-translation-core/target/surefire-reports/TEST-*.xml", allowEmptyResults: allowEmptyResults
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
                    if (params.runReleaseBuild) {
                        imageRepo = "docker-products"
                    }
                    def imageName = "${imageHost}/${imageRepo}/gentics/cms"
                    def imageNameWithTag = "${imageName}:${branchName}"

                    authDockerRegistry("docker.gentics.com", "docker.gentics.com")
                    authDockerRegistry("docker.gentics.com", "push.docker.gentics.com")

                    sh "docker --version"
                    sh "cd cms-server ; docker build --network=host -t ${imageNameWithTag} ."

                    if (!params.omitScan) {
                        scanImage image: imageNameWithTag, exitCode: 1
                    }
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

        stage("Build Helm Chart") {
            when {
                expression {
                    return env.BUILD_SKIPPED != "true" && params.runHelmBuild
                }
            }
            steps {
                script {
                    sh ".jenkins/build-helm-chart.sh ${helmVersion}"

                    if (Boolean.valueOf(params.deploy) || Boolean.valueOf(params.deployTesting)) {
                        if (Boolean.valueOf(params.deployTesting)) {
                            helmRepository = helmRepositoryTest;
                        }

                        withCredentials([usernamePassword(credentialsId: 'docker.gentics.com', usernameVariable: 'repoUsername', passwordVariable: 'repoPassword')]) {
                            def helmPackageFilename = "cms-${helmVersion}.tgz"
                            echo "Deploying CMS Helm Chart"
                            sh "curl -u ${repoUsername}:${repoPassword} ${helmRepository} --upload-file target/${helmPackageFilename}"
                        }
                    }
                }
            }
        }

        stage("UI Integration Tests") {
			when {
				expression {
                    // Needs built docker images to be deployed (as they are used in another job)
                    // And only if the tests should be performed as well
					return env.BUILD_SKIPPED != "true" && params.runDockerBuild && (params.deploy || params.deployTesting) && params.integrationTests
				}
			}

            steps {
                script {
                    def integrationCmsVersion = tagName != null && !tagName.isEmpty() ? tagName : branchName;
                    def testJob = build(job: '/CMP_DEV/CMS/cms-ui-integration-tests/' + branchName,
                        parameters: [
                            string(name: 'variant', value: 'EE'),
                            string(name: 'cmsVersion', value: integrationCmsVersion),
                            // TODO: Get Mesh Version from POM?
                            // string(name: 'meshVersion', value: '2.1.0')
                        ],
                        wait: true
                    )

                    if ( testJob.getBuildVariables()["BUILD_SKIPPED"] == "true" ) {
                        echo "Integration Tests finished with an error"
                    } else {
                        echo "Integration Tests finished successfully"
                    }
                }
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

    post {
        always {
            updateGitlabCommitCurrentBuildStatus name: 'Jenkins build'
        }
    }
}
