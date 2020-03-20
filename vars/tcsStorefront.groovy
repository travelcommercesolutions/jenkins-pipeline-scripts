#!groovy
import jobs.scripts.*

def call(body) {
	def config = [:]
	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()

	node
	{
		properties([disableConcurrentBuilds()])
		projectType = config.projectType

	    def storeName = config.sampleStore
		projectType = "MSBUILD"
		def solution = config.solution
		solution = "VirtoCommerce.Storefront.sln"

		def dockerTag =  "latest"
		def buildOrder = Utilities.getNextBuildOrder(this)
		def websiteDir = 'VirtoCommerce.Storefront'
		def webProject = 'VirtoCommerce.Storefront\\VirtoCommerce.Storefront.csproj'
		def prefix = Utilities.getRepoNamePrefix(this)
		def zipArtifact = "${prefix}-storefront"
		def deployScript = 'VC-Storefront2AzureDev.ps1'

		try
		{
			stage('Checkout')
			{
				timestamps
				{
					if(BRANCH_NAME != 'dev')
					{
						deleteDir()
					}
					checkout scm
				}
			}

			stage('Build')
			{
				timestamps
				{
					Packaging.startAnalyzer(this)
					Packaging.runBuild(this, solution)
				}
			}

			def version = Utilities.getAssemblyVersion(this, webProject)
			def dockerImage

			if(!Utilities.isPullRequest(this))
			{
				stage('Packaging')
				{
					timestamps
					{
						Packaging.createReleaseArtifact(this, version, webProject, zipArtifact, websiteDir)
						if (env.BRANCH_NAME == 'dev' || env.BRANCH_NAME == 'qa' || env.BRANCH_NAME == 'master')
						{
							def websitePath = Utilities.getWebPublishFolder(this, websiteDir)
							powershell "Expand-Archive -Path artifacts\\*.zip -DestinationPath artifacts\\${websiteDir} -Force" //for docker
						}
					}
				}
			}

            def tests = Utilities.getTestDlls(this)
			if(tests.size() > 0)
			{
				stage('Unit Tests')
				{
					timestamps
					{
						// Packaging.runUnitTests(this)
					}
				}
			}

			stage('Code Analysis')
			{
				timestamps
				{
					Packaging.endAnalyzer(this)
					Packaging.checkAnalyzerGate(this)
				}
			}

			def artifacts = findFiles(glob: 'artifacts/*.zip')
			if(params.themeResultZip != null){
                for(artifact in artifacts){
                    bat "copy /Y \"${artifact.path}\" \"${params.themeResultZip}\""
                }
            }

			def themePath = "${env.WORKSPACE}@tmp\\theme.zip"

			stage('Publish')
			{
				timestamps
				{
					def notes = Utilities.getReleaseNotes(this, webProject)
					if (env.BRANCH_NAME == 'dev' || env.BRANCH_NAME == 'master')
					{
						if(!Utilities.isPullRequest(this))
						{
							Packaging.saveArtifact(this, 'tcs', 'storefront', '', artifacts[0].path) // config.sampleStore for projects w def store
						}
						if (env.BRANCH_NAME == 'master') {
							Packaging.publishRelease(this, version, notes) // publish artifacts to github releases
						}
						if (env.BRANCH_NAME == 'dev') {
							Packaging.publishRelease(this, java.time.LocalDateTime.now(), notes) // publish artifacts to github releases
						}
					}
					// Utilities.runSharedPS(this, "${deployScript}", "-Prefix ${prefix}")
				}
			}

			stage('Cleanup')
			{
				timestamps
				{
					bat "dotnet build-server shutdown"
					// bat "docker image prune --force"
				}
			}
		}
		catch(any)
		{
			currentBuild.result = 'FAILURE'
			// Utilities.notifyBuildStatus(this, currentBuild.result)
			throw any //rethrow exception to prevent the build from proceeding
		}
		finally
		{
			//Packaging.stopDockerTestEnvironment(this, dockerTag)
			//Utilities.generateAllureReport(this)
			// bat "docker image prune --force"
			if(currentBuild.result != 'FAILURE')
			{
				// step([$class: 'Mailer', notifyEveryUnstableBuild: true, recipients: emailextrecipients([[$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']])])
			}
			else
			{
				def log = currentBuild.rawBuild.getLog(300)
				def failedStageLog = Utilities.getFailedStageStr(log)
				def failedStageName = Utilities.getFailedStageName(failedStageLog)
				def mailBody = Utilities.getMailBody(this, failedStageName, failedStageLog)
				// emailext body:mailBody, subject: "${env.JOB_NAME}:${env.BUILD_NUMBER} - ${currentBuild.currentResult}", recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']]
			}
			Utilities.cleanPRFolder(this)
        }
		//Utilities.notifyBuildStatus(this, currentBuild.result)
    }
}
