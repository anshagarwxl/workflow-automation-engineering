# Switch the simulated payment service: .\fault.ps1 down | up | slow | status
param([string]$Mode = "status")
$Mode = $Mode.ToUpper()
if ($Mode -eq "STATUS") { Invoke-RestMethod "http://localhost:8090/admin/faults" | ConvertTo-Json; exit }
Invoke-RestMethod -Method Put "http://localhost:8090/admin/faults/payment?mode=$Mode" | ConvertTo-Json
