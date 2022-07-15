param([string]$resourceGroupName, [string]$privateEndpointName, [string]$resourceName, [string]$vNetName, [string]$subnet)

$ResourceGroupName = "$resourceGroupName"
$VNetName = "$vNetName"
$SubnetName = "$subnet"
$PrivateEndpointName = "$privateEndpointName"

$Location = "westeurope"

$virtualNetwork = Get-AzVirtualNetwork -ResourceGroupName  $ResourceGroupName -Name $VNetName  
$subnet = $virtualNetwork 
 
$privateEndpoint = New-AzPrivateEndpoint -ResourceGroupName $ResourceGroupName -Name $PrivateEndpointName -Location $Location -Subnet  $subnet