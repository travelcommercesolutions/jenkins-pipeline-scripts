Write-Host "CREATING APP SERVICE PLAN";
$invocation = $MyInvocation.MyCommand.Path
$directorypath = Split-Path $invocation
$templateFilePath=$directorypath+"\..\/createAppServicePlan.json"
New-AzureRmResourceGroupDeployment -ResourceGroupName $resourceGroupName -TemplateFile $templateFilePath -aseRgName $aseRgName -location $location -tags $tags -serverfarmName $aspName -aseName $aseName -tierLevel $tierLevel -ErrorAction Stop
