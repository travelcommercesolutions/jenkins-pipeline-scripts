param([string]$resourceGroupName)

$resourceGroup = "$resourceGroupName"
$location = "westeurope"
$templateFile=$directorypath+"\..\/createStorageAccount.json"

New-AzResourceGroupDeployment -ResourceGroupName $resourceGroup -TemplateFile $templateFile