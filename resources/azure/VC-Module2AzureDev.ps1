param(
    [parameter(Mandatory = $true)]
    $Prefix
)

. $PSScriptRoot\utilities.ps1 

# Get Module Name

$Path = Get-Childitem -Recurse -Path "${env:WORKSPACE}\" -File -Include module.manifest

# Read in the file contents and return the version node's value.
[ xml ]$fileContents = Get-Content -Path $Path -Raw
$ModuleName = Select-Xml -Xml $fileContents -XPath "/module/id"

# Get Module Zip File

$Path2Zip = Get-Childitem -Recurse -Path "${env:WORKSPACE}\artifacts\" -File -Include *.zip

# Upload Module Zip File to Azure

$ApplicationID ="${env:AzureAppID}"
$APIKey = ConvertTo-SecureString "${env:AzureAPIKey}" -AsPlainText -Force
$psCred = New-Object System.Management.Automation.PSCredential($ApplicationID, $APIKey)
$TenantID = "${env:AzureTenantID}"
$SubscriptionID = Get-EnvVar -Prefix $Prefix -Name "AzureSubscriptionIDDev"

Add-AzureRmAccount -Credential $psCred -TenantId $TenantID -ServicePrincipal
Select-AzureRmSubscription -SubscriptionId $SubscriptionID

$DestResourceGroupName = Get-EnvVar -Prefix $Prefix -Name "AzureResourceGroupNameDev"
$DestWebAppName = Get-EnvVar -Prefix $Prefix -Name "AzureWebAppAdminNameDev"
$DestKuduDelPath = "https://$DestWebAppName.scm.azurewebsites.net/api/vfs/site/wwwroot/modules/$ModuleName/?recursive=true"
$DestKuduPath = "https://$DestWebAppName.scm.azurewebsites.net/api/zip/site/wwwroot/modules/$ModuleName/"

function Get-AzureRmWebAppPublishingCredentials($DestResourceGroupName, $DestWebAppName, $slotName = $null){
	if ([string]::IsNullOrWhiteSpace($slotName)){
        $ResourceType = "Microsoft.Web/sites/config"
		$DestResourceName = "$DestWebAppName/publishingcredentials"
	}
	else{
        $ResourceType = "Microsoft.Web/sites/slots/config"
		$DestResourceName = "$DestWebAppName/$slotName/publishingcredentials"
	}
	$DestPublishingCredentials = Invoke-AzureRmResourceAction -ResourceGroupName $DestResourceGroupName -ResourceType $ResourceType -ResourceName $DestResourceName -Action list -ApiVersion 2015-08-01 -Force
    	return $DestPublishingCredentials
}

function Get-KuduApiAuthorisationHeaderValue($DestResourceGroupName, $DestWebAppName){
    $DestPublishingCredentials = Get-AzureRmWebAppPublishingCredentials $DestResourceGroupName $DestWebAppName
    return ("Basic {0}" -f [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes(("{0}:{1}" -f $DestPublishingCredentials.Properties.PublishingUserName, $DestPublishingCredentials.Properties.PublishingPassword))))
}

$DestKuduApiAuthorisationToken = Get-KuduApiAuthorisationHeaderValue $DestResourceGroupName $DestWebAppName

Write-Host "Stop $DestWebAppName"

Stop-AzureRmWebApp -ResourceGroupName $DestResourceGroupName -Name $DestWebAppName | Select Name,State
Start-Sleep -s 5

Write-Host "Deleting Files at $DestKuduDelPath"
try{
    Invoke-RestMethod -Uri $DestKuduDelPath -Headers @{"Authorization"=$DestKuduApiAuthorisationToken;"If-Match"="*"} -Method DELETE
}
catch{
    Write-Host "Error on Deleting Files"
}


Start-Sleep -s 15

Write-Host "Uploading File $Path2Zip to $DestKuduPath"

Invoke-RestMethod -Uri $DestKuduPath `
                        -Headers @{"Authorization"=$DestKuduApiAuthorisationToken;"If-Match"="*"} `
                        -Method PUT `
                        -InFile $Path2Zip `
                        -ContentType "multipart/form-data"

Start-Sleep -s 15

Write-Host "Start $DestWebAppName"

Start-AzureRmWebApp -ResourceGroupName $DestResourceGroupName -Name $DestWebAppName | Select Name,State