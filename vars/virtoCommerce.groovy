#!groovy
import groovy.json.*
import groovy.util.*
import jobs.scripts.*

	def call(body){

	// evaluate the body block, and collect configuration into the object
	def config = [:]
	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()

	node
	{
		properties([disableConcurrentBuilds()])
	    def deployScript = 'VC-2AzureQA.ps1'
		def dockerTag = "${env.BRANCH_NAME}-branch"
		def buildOrder = Utilities.getNextBuildOrder(this)
		projectType = config.projectType

		def SETTINGS
		def settingsFileContent
		configFileProvider([configFile(fileId: 'shared_lib_settings', variable: 'SETTINGS_FILE')]) {
			settingsFileContent = readFile(SETTINGS_FILE)
		}
		SETTINGS = new Settings(settingsFileContent)
		SETTINGS.setRegion('virtocommerce')
		SETTINGS.setEnvironment(env.BRANCH_NAME)

		if (env.BRANCH_NAME == 'deploy' || env.BRANCH_NAME == 'dev-vc-new-design'){
			deployScript = 'VC-2AzureDEV.ps1'
			dockerTag = "latest"
		}

		try{
			//Utilities.notifyBuildStatus(this, "started")
			stage('Checkout'){
				timestamps{
					deleteDir()
					checkout scm
				}
			}

			if(Utilities.checkAndAbortBuild(this))
			{
				return true
			}

			stage('Copy to DEV-VC') {
				timestamps {
					switch(env.BRANCH_NAME) {
						case 'deploy':
							def stagingName = "deploy"
							def storeName = "cms-content"
							Utilities.runSharedPS(this, "${deployScript}", "-StagingName ${stagingName} -StoreName ${storeName} -AzureBlobName ${SETTINGS['azureBlobName']} -AzureBlobKey ${SETTINGS['azureBlobKey']} -WebAppName ${SETTINGS['webAppName']} -ResourceGroupName ${SETTINGS['resourceGroupName']} -SubscriptionID ${SETTINGS['subscriptionID']} -TokenSas ${SETTINGS['tokenSas']}")
							break
						case 'dev-vc-new-design':
							def stagingName = "dev-vc-new-design"
							def storeName = "cms-content"
							Utilities.runSharedPS(this, "${deployScript}", "-StagingName ${stagingName} -StoreName ${storeName} -AzureBlobName ${SETTINGS['azureBlobName']} -AzureBlobKey ${SETTINGS['azureBlobKey']} -WebAppName ${SETTINGS['webAppName']} -ResourceGroupName ${SETTINGS['resourceGroupName']} -SubscriptionID ${SETTINGS['subscriptionID']} -TokenSas ${SETTINGS['tokenSas']}")
							break
					}
				}
			}

			stage('E2E'){
				timestamps{
					timeout(time: 20, unit: 'MINUTES'){
						try{
							Utilities.runE2E(this)
							def e2eStatus = "E2E Success"
						}
						catch(any){
							e2eStatus = "E2E Failed"
						}
						finally{
							def allureReportAddress = "${env.BUILD_URL}/allure"
							//Utilities.notifyBuildStatus(this, SETTINGS['of365hook'], "${allureReportAddress}", "${e2eStatus}")
							msg = "${e2eStatus}."
							if(!(e2eStatus == 'E2E Success')) {
								input(message: msg, submitter: env.APPROVERS)
							}
						}
					}
				}
			}

			stage('Copy to PROD-VC') {
				timestamps {
					deployScript = 'VC-2AzurePROD.ps1'
					switch(env.BRANCH_NAME){
						case 'deploy':
							def stagingName = "deploy"
							def storeName = "cms-content"
							Utilities.runSharedPS(this, "${deployScript}", "-StagingName ${stagingName} -StoreName ${storeName} -AzureBlobName ${SETTINGS['azureBlobNameProd']} -AzureBlobKey ${SETTINGS['azureBlobKeyProd']} -WebAppName ${SETTINGS['webAppNameProd']} -ResourceGroupName ${SETTINGS['resourceGroupNameProd']} -SubscriptionID ${SETTINGS['subscriptionID']} -TokenSas ${SETTINGS['tokenSasProd']}")
							break
						case 'dev-vc-new-design':
							def stagingName = "dev-vc-new-design"
							def storeName = "cms-content"
							Utilities.runSharedPS(this, "${deployScript}", "-StagingName ${stagingName} -StoreName ${storeName} -AzureBlobName ${SETTINGS['azureBlobNameProd']} -AzureBlobKey ${SETTINGS['azureBlobKeyProd']} -WebAppName ${SETTINGS['webAppNameProd']} -ResourceGroupName ${SETTINGS['resourceGroupNameProd']} -SubscriptionID ${SETTINGS['subscriptionID']} -TokenSas ${SETTINGS['tokenSasProd']}")
							break
					}
				}
			}
			
			stage('Cleanup') {
				timestamps { 
					Packaging.cleanSolutions(this)
				}
			}
		}
		catch (any) {
			currentBuild.result = 'FAILURE'
			//Utilities.notifyBuildStatus(this, currentBuild.result)
			throw any //rethrow exception to prevent the build from proceeding
		}
		finally {
			Packaging.stopDockerTestEnvironment(this, dockerTag)
			Utilities.generateAllureReport(this)
			step([$class: 'LogParserPublisher',
				  failBuildOnError: false,
				  parsingRulesPath: env.LOG_PARSER_RULES,
				  useProjectRule: false])
			if(currentBuild.result != 'FAILURE') {
				//step([$class: 'Mailer', notifyEveryUnstableBuild: true, recipients: emailextrecipients([[$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']])])
			}
			else {
				def log = currentBuild.rawBuild.getLog(300)
				def failedStageLog = Utilities.getFailedStageStr(log)
				def failedStageName = Utilities.getFailedStageName(failedStageLog)
				def mailBody = Utilities.getMailBody(this, failedStageName, failedStageLog)
				emailext body:mailBody, subject: "${env.JOB_NAME}:${env.BUILD_NUMBER} - ${currentBuild.currentResult}", recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']]
			}
		}

		//step([$class: 'GitHubCommitStatusSetter', statusResultSource: [$class: 'ConditionalStatusResultSource', results: []]])
	}
}
