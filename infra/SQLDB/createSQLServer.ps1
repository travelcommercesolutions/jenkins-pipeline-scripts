Write-Host "CREATING SQL Server";
$invocation = $MyInvocation.MyCommand.Path
$directorypath = Split-Path $invocation
try {
	$appServicePlan = Get-AzureRmAppServicePlan -Name $aspName -ResourceGroupName $rgGroupName -ErrorAction Stop
} catch {
	$templateFile=$directorypath+"\..\/createSQLServer.json"
	$dbpassword_secret = ConvertTo-SecureString $sqlServerAdminPass -AsPlainText -Force
	New-AzureRmResourceGroupDeployment -ResourceGroupName $resourceGroupName -TemplateFile $templateFile -serversName $sqlServerName -databases_login $sqlServerAdminUser -databases_password $dbpassword_secret
}