param(
    $SubscriptionID,
    $ResourceGroupName,
    $TemplateFile,
    $TemplateParametersFile
)

Write-Output "SubscriptionID $SubscriptionID, ResourceGroupName $ResourceGroupName, TemplateFile $TemplateFile, TemplateParametersFile $TemplateParametersFile"

$ApplicationID ="${env:AzureAppID}"
$APIKey = ConvertTo-SecureString "${env:AzureAPIKey}" -AsPlainText -Force
$psCred = New-Object System.Management.Automation.PSCredential($ApplicationID, $APIKey)
$TenantID = "${env:AzureTenantID}"

Add-AzureRmAccount -Credential $psCred -TenantId $TenantID -ServicePrincipal
Select-AzureRmSubscription -SubscriptionId $SubscriptionID

$ErrorMessages = Test-AzureRmResourceGroupDeployment -ResourceGroupName $ResourceGroupName -TemplateFile $TemplateFile -TemplateParameterFile $TemplateParametersFile 
if ($ErrorMessages) {
    Write-Output '', 'Validation returned the following errors:', @($ErrorMessages), '', 'Template is invalid.'
}
else {
    Write-Output '', 'Template is valid.'
}

New-AzureRmResourceGroupDeployment -Name ((Get-ChildItem $TemplateFile).BaseName + '-' + ((Get-Date).ToUniversalTime()).ToString('MMdd-HHmm')) `
                                       -ResourceGroupName $ResourceGroupName `
                                       -TemplateFile $TemplateFile `
                                       -TemplateParameterFile $TemplateParametersFile `
                                       -Force -Verbose `
                                       -ErrorVariable ErrorMessages
if ($ErrorMessages) {
    Write-Output '', 'Template deployment returned the following errors:', @(@($ErrorMessages) | ForEach-Object { $_.Exception.Message.TrimEnd("`r`n") })
}
