Param(
	[parameter(Mandatory = $true)]
    $AppName,
    $ResourceGroupName,
    $SubscriptionID,
    $PlatformPath = $null,
	$ModulesPath = $null
)

$ApplicationID ="${env:AzureAppID}"
$APIKey = ConvertTo-SecureString "${env:AzureAPIKey}" -AsPlainText -Force
$psCred = New-Object System.Management.Automation.PSCredential($ApplicationID, $APIKey)
$TenantID = "${env:AzureTenantID}"

Add-AzureRmAccount -Credential $psCred -TenantId $TenantID -ServicePrincipal
Select-AzureRmSubscription -SubscriptionId $SubscriptionID

# Getting Publish Profile
Write-Output "Getting publishing profile for $AppName app"
$tmpPublishProfile = [System.IO.Path]::GetTempFileName() + ".xml"
$xml = Get-AzureRmWebAppPublishingProfile -Name $AppName `
           -ResourceGroupName $ResourceGroupName `
           -OutputFile $tmpPublishProfile -Format WebDeploy -ErrorAction Stop

$msdeploy = "${env:MSDEPLOY_DIR}\msdeploy.exe"

if($PlatformPath){
    #DOWNLOAD PLATFORM
    Write-Output "DOWNLOAD PLATFORM"
    $sourcewebapp_msdeployUrl = "https://${AppName}.scm.azurewebsites.net/msdeploy.axd?site=${AppName}"
    & $msdeploy -verb:sync -source:contentPath="D:\home\site\wwwroot\platform",computerName=$sourcewebapp_msdeployUrl,publishSettings=$tmpPublishProfile -dest:contentPath=$PlatformPath

    #Removing App_Data
    Remove-Item $PlatformPath\\App_Data -Recurse -Force -ErrorAction Continue
}

if($ModulesPath){
    # DOWNLOAD MODULES
    Write-Output "DOWNLOAD MODULES"
    $sourcewebapp_msdeployUrl = "https://${AppName}.scm.azurewebsites.net/msdeploy.axd?site=${AppName}"
    & $msdeploy -verb:sync -source:contentPath="D:\home\site\wwwroot\modules",computerName=$sourcewebapp_msdeployUrl,publishSettings=$tmpPublishProfile -dest:contentPath=$ModulesPath
}

Remove-Item -Path $tmpPublishProfile -Force