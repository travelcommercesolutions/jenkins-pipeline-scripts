Write-Host "CONNECTING TO AZURE";
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$password = ConvertTo-SecureString "$ENV:AZURE_PASSWORD" -AsPlainText -Force
$credentials = New-Object -TypeName pscredential -ArgumentList "$ENV:AZURE_USER_ID@softteco.com", $password
Login-AzureRmAccount -Credential $credentials -ServicePrincipal -TenantId "$ENV:AZURE_TENANT_ID" -Subscription "$ENV:AZURE_SUBSCRIPTION_ID"
#need to add $ENV: variables to jenkins