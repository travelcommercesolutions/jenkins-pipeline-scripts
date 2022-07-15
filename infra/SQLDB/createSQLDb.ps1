Write-Host "CREATING DATABASE";
$invocation = $MyInvocation.MyCommand.Path
$directorypath = Split-Path $invocation
$templateFilePath = $directorypath+"/../\createSQLDb.json"

# !!! elastic pool probably needed


$deploymentLocation = $null

New-AzureRmResourceGroupDeployment `
    -Name "${databaseName}" `
    -tags $tags `
    -ResourceGroupName $databaseResourceGroup `
    -TemplateFile $templateFilePath `
    -sql_server_name $sqlServerName `
    -database_name $databaseName `
    -database_size $databaseSize `
    -location $deploymentLocation `
    -ErrorAction Stop

Write-Host "CREATING USERS";

$sqlServerAdminUser = "tcs-admin"

$SQLDBName = "$databaseName"
$connString= "Server = "+$sqlServerName+".database.windows.net; Database = "+$SQLDBName+"; User ID = '"+$sqlServerAdminUser+"'; Password = '"+$sqlServerAdminPass+"';"
Invoke-Sqlcmd -ConnectionString $connString -Query "CREATE LOGIN [tcs-admin-${appName}] WITH PASSWORD=N'$dbUserPassSuffix'" -ErrorAction Ignore

