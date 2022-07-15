param([string]$resourceGroupName, [string]$appInsightsName, [string]$workspaceName)
$invocation = $MyInvocation.MyCommand.Path
$directorypath = Split-Path $invocation
$template=$directorypath+"\..\/AppInsights.json"
$resourceId = (Get-AzureRmOperationalInsightsWorkspace -Name $workspaceName -ResourceGroupName $resourceGroupName).ResourceId

Write-Host "CREATING APP INSIGHT";
New-AzureRmResourceGroupDeployment -ResourceGroupName $resourceGroupName -TemplateFile $template -appInsightsName $appInsightsName -workspaceResourceId $resourceId -ErrorAction Stop
