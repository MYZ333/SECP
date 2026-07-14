param(
    [string]$HostName = "127.0.0.1",
    [ValidateRange(1, 65535)]
    [int]$Port = 8000,
    [string]$Tenant = "default_tenant",
    [string]$Database = "default_database",
    [string]$KnowledgeCollection = "medical_knowledge",
    [string]$MemoryCollection = "long_term_memory",
    [switch]$AllowEmptyKnowledge
)

$ErrorActionPreference = "Stop"
$baseUrl = "http://${HostName}:$Port"

try {
    $heartbeat = Invoke-RestMethod -Uri "$baseUrl/api/v2/heartbeat" -Method Get -TimeoutSec 5
    $version = Invoke-RestMethod -Uri "$baseUrl/api/v2/version" -Method Get -TimeoutSec 5
} catch {
    throw "Chroma is not ready at $baseUrl. $($_.Exception.Message)"
}

if ($null -eq $heartbeat.PSObject.Properties["nanosecond heartbeat"].Value) {
    throw "The service at $baseUrl responded, but it is not a compatible Chroma v2 API."
}

Write-Host "Chroma is healthy." -ForegroundColor Green
Write-Host "URL: $baseUrl"
Write-Host "Version: $version"
Write-Host "Heartbeat: $($heartbeat.PSObject.Properties["nanosecond heartbeat"].Value)"

$tenantPart = [Uri]::EscapeDataString($Tenant)
$databasePart = [Uri]::EscapeDataString($Database)
$collectionsUrl = "$baseUrl/api/v2/tenants/$tenantPart/databases/$databasePart/collections"
try {
    $collections = Invoke-RestMethod -Uri $collectionsUrl -Method Get -TimeoutSec 10
} catch {
    throw "Unable to list Chroma collections for tenant=$Tenant database=$Database. $($_.Exception.Message)"
}

Write-Host "Tenant: $Tenant"
Write-Host "Database: $Database"
if ($collections.Count -eq 0) {
    throw "Chroma is healthy but has no collections. Start the Spring Boot backend to create them."
}

$counts = @{}
foreach ($collection in $collections) {
    $countUrl = "${collectionsUrl}/$($collection.id)/count"
    $count = Invoke-RestMethod -Uri $countUrl -Method Get -TimeoutSec 10
    $counts[$collection.name] = [long]$count
    Write-Host ("Collection: {0}  Count: {1}" -f $collection.name, $count)
}

if (-not $counts.ContainsKey($KnowledgeCollection)) {
    throw "Required knowledge collection '$KnowledgeCollection' does not exist."
}
if (-not $AllowEmptyKnowledge -and $counts[$KnowledgeCollection] -le 0) {
    throw "Knowledge collection '$KnowledgeCollection' is empty. Import and publish seed knowledge from the admin knowledge page, or rerun with -AllowEmptyKnowledge."
}
if (-not $counts.ContainsKey($MemoryCollection)) {
    throw "Required memory collection '$MemoryCollection' does not exist."
}
Write-Host "Chroma collections are initialized correctly." -ForegroundColor Green
