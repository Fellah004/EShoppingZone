$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

Write-Host "====================================================" -ForegroundColor Cyan
Write-Host " TESTING THYMELEAF WEB TEMPLATES & MVC CONTROLLERS  " -ForegroundColor Cyan
Write-Host "====================================================" -ForegroundColor Cyan

$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

# 1. Home Page
$homePage = Invoke-WebRequest -Uri "http://localhost:8088/" -WebSession $session -UseBasicParsing
Write-Host " -> Home Page: $($homePage.StatusCode) OK (Length: $($homePage.Content.Length))" -ForegroundColor Green
if ($homePage.Content -notmatch "Explore Products at") { throw "Home page content mismatch" }

# 2. Products Catalog
$catalog = Invoke-WebRequest -Uri "http://localhost:8088/products" -WebSession $session -UseBasicParsing
Write-Host " -> Catalog Page: $($catalog.StatusCode) OK (Length: $($catalog.Content.Length))" -ForegroundColor Green
if ($catalog.Content -notmatch "Product Catalog") { throw "Catalog page content mismatch" }

# 3. Register Merchant via Web Form
$mEmail = "merchant_web_$((Get-Date).Ticks)@eshoppingzone.com"
$regParams = @{
    fullName = "Heritage Silk & Crafts"
    email = $mEmail
    password = "Password@123"
    mobileNumber = "9977$([math]::Round((Get-Random -Minimum 100000 -Maximum 999999)))"
    role = "MERCHANT"
    "address.streetName" = "Brigade Road"
    "address.city" = "Bengaluru"
    "address.state" = "Karnataka"
    "address.pincode" = "560001"
}

$regRes = Invoke-WebRequest -Uri "http://localhost:8088/register" -Method Post -Body $regParams -WebSession $session -UseBasicParsing
Write-Host " -> Merchant Register & Auto-Login: $($regRes.StatusCode) OK" -ForegroundColor Green

# 4. Merchant Dashboard
$dash = Invoke-WebRequest -Uri "http://localhost:8088/merchant/dashboard" -WebSession $session -UseBasicParsing
Write-Host " -> Merchant Dashboard: $($dash.StatusCode) OK" -ForegroundColor Green
if ($dash.Content -notmatch "Merchant Workspace") { throw "Dashboard content mismatch" }

# 5. Merchant Products Page
$mProds = Invoke-WebRequest -Uri "http://localhost:8088/merchant/products" -WebSession $session -UseBasicParsing
Write-Host " -> Merchant Products: $($mProds.StatusCode) OK" -ForegroundColor Green
if ($mProds.Content -notmatch "My Products Catalog") { throw "Merchant products page mismatch" }

# 6. Merchant Add Product Page
$addPage = Invoke-WebRequest -Uri "http://localhost:8088/merchant/products/add" -WebSession $session -UseBasicParsing
Write-Host " -> Merchant Add Product Page: $($addPage.StatusCode) OK" -ForegroundColor Green

# 7. Merchant Sales Ledger Page
$salesPage = Invoke-WebRequest -Uri "http://localhost:8088/merchant/sales" -WebSession $session -UseBasicParsing
Write-Host " -> Merchant Sales Ledger Page: $($salesPage.StatusCode) OK" -ForegroundColor Green
if ($salesPage.Content -notmatch "Customer Sales Ledger") { throw "Merchant sales page mismatch" }

Write-Host "`n====================================================" -ForegroundColor Green
Write-Host " ALL WEB TEMPLATES & MVC CONTROLLERS TESTED CLEANLY! " -ForegroundColor Green
Write-Host "====================================================" -ForegroundColor Green
