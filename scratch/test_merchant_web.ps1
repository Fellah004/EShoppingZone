$ErrorActionPreference = "Continue"

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "EShoppingZone - Merchant Web Interface and Cookie Session Tests" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan

$WebUrl = "http://localhost:8088"
$ProfileUrl = "http://localhost:8081"

# 1. Register Merchant
$MerchEmail = "web_merchant_" + (Get-Random -Minimum 1000 -Maximum 9999) + "@example.com"
$MerchRegBody = @{
    fullName = "Devi Enterprises"
    email = $MerchEmail
    mobileNumber = "9874" + (Get-Random -Minimum 100000 -Maximum 999999)
    password = "MerchantPassword123!"
    role = "MERCHANT"
    gender = "Female"
    dateOfBirth = "1990-03-12"
    about = "Apparel and personal care retailer"
    address = @{
        houseNumber = "Suite 201"
        streetName = "MG Road"
        colonyName = "Central"
        city = "Bengaluru"
        state = "Karnataka"
        pinCode = 560001
    }
} | ConvertTo-Json

$merchRegRes = Invoke-RestMethod -Uri "$ProfileUrl/api/profile/register" -Method Post -Body $MerchRegBody -ContentType "application/json"
Write-Host "Registered Merchant: ID=$($merchRegRes.profileId), Email=$MerchEmail" -ForegroundColor Green

# 2. Login via Web Controller Form
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$escapedEmail = [System.Uri]::EscapeDataString($MerchEmail)
$escapedPass  = [System.Uri]::EscapeDataString("MerchantPassword123!")
$loginBody = "email=$escapedEmail&password=$escapedPass"

$loginRes = Invoke-WebRequest -Uri "$WebUrl/login" -Method Post -Body $loginBody -ContentType "application/x-www-form-urlencoded" -WebSession $session -UseBasicParsing -MaximumRedirection 5
Write-Host "Web Login Redirect -> $($loginRes.BaseResponse.ResponseUri.AbsoluteUri)" -ForegroundColor Green

# 3. Access Merchant Dashboard
$dashRes = Invoke-WebRequest -Uri "$WebUrl/merchant/dashboard" -Method Get -WebSession $session -UseBasicParsing
Write-Host "Merchant Dashboard -> HTTP $($dashRes.StatusCode) (Contains 'Merchant Dashboard': $($dashRes.Content.Contains('Merchant Dashboard')))" -ForegroundColor Green

# 4. Access My Products
$prodRes = Invoke-WebRequest -Uri "$WebUrl/merchant/products" -Method Get -WebSession $session -UseBasicParsing
Write-Host "Merchant My Products -> HTTP $($prodRes.StatusCode) (Contains 'My Products Catalog': $($prodRes.Content.Contains('My Products Catalog')))" -ForegroundColor Green

# 5. Access Add Product Page
$addRes = Invoke-WebRequest -Uri "$WebUrl/merchant/products/add" -Method Get -WebSession $session -UseBasicParsing
Write-Host "Merchant Add Product -> HTTP $($addRes.StatusCode) (Contains 'Add New Product': $($addRes.Content.Contains('Add New Product')))" -ForegroundColor Green

# 6. Attempt Customer Checkout as Merchant (Should be redirected to /merchant/dashboard)
$cartRes = Invoke-WebRequest -Uri "$WebUrl/cart" -Method Get -WebSession $session -UseBasicParsing -MaximumRedirection 5
Write-Host "Merchant Cart Access -> Redirected to: $($cartRes.BaseResponse.ResponseUri.AbsoluteUri)" -ForegroundColor Green

Write-Host "`n================================================================" -ForegroundColor Cyan
Write-Host "MERCHANT WEB INTERFACE TESTS COMPLETED!" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
