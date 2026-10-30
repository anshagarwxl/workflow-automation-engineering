# Lab 2 - Order processing: start the automatic test scenarios and print the live results table.
# Usage: .\scripts\run-scenarios.ps1            (all automatic scenarios)
#        .\scripts\run-scenarios.ps1 <scenario> (one scenario, e.g. 01-...)
param([string]$Scenario = "")
$App = if ($env:APP) { $env:APP } else { "http://localhost:8092" }

Write-Host "== Scenarios available"
Invoke-RestMethod "$App/api/scenarios" | ConvertTo-Json -Depth 5

if ($Scenario) {
  Write-Host "== Starting scenario $Scenario"
  Invoke-RestMethod -Method Post "$App/api/scenarios/$Scenario/start" | ConvertTo-Json
} else {
  Write-Host "== Starting all automatic scenarios (failure/recovery ones are started manually, see README)"
  Invoke-RestMethod -Method Post "$App/api/scenarios/start-all" | ConvertTo-Json
}

Write-Host "== Waiting 15 s for workers, connectors and Operate indexing..."
Start-Sleep -Seconds 15
Write-Host "== Results (complete pending human tasks in Tasklist, then call $App/api/scenarios/results.md again)"
Invoke-RestMethod "$App/api/scenarios/results.md"
