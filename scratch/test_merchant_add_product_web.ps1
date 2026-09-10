$ErrorActionPreference = "Continue"

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "EShoppingZone - Merchant Add Product Web Form End-to-End Test" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan

$WebUrl = "http://localhost:8088"
$ProfileUrl = "http://localhost:8081"

# 1. Register Merchant
$MerchEmail = "web_merchant_seller_" + (Get-Random -Minimum 1000 -Maximum 9999) + "@example.com"
$MerchRegBody = @{
    fullName = "Apex Electronics Hub"
    email = $MerchEmail
    mobileNumber = "9872" + (Get-Random -Minimum 100000 -Maximum 999999)
    password = "MerchantPassword123!"
    role = "MERCHANT"
    gender = "Male"
    dateOfBirth = "1987-11-05"
    about = "Authorized seller of audio gear"
    address = @{
        houseNumber = "Plot 88"
        streetName = "Tech Boulevard"
        colonyName = "Electronic City"
        city = "Bengaluru"
        state = "Karnataka"
        pinCode = 560100
    }
} | ConvertTo-Json

$merchRegRes = Invoke-RestMethod -Uri "$ProfileUrl/api/profile/register" -Method Post -Body $MerchRegBody -ContentType "application/json"
Write-Host "Registered Merchant: ID=$($merchRegRes.profileId), Email=$MerchEmail" -ForegroundColor Green

# 2. Login
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$escapedEmail = [System.Uri]::EscapeDataString($MerchEmail)
$escapedPass  = [System.Uri]::EscapeDataString("MerchantPassword123!")
$loginBody = "email=$escapedEmail&password=$escapedPass"

$loginRes = Invoke-WebRequest -Uri "$WebUrl/login" -Method Post -Body $loginBody -ContentType "application/x-www-form-urlencoded" -WebSession $session -UseBasicParsing -MaximumRedirection 5
Write-Host "Logged In Successfully as Merchant -> $($loginRes.BaseResponse.ResponseUri.AbsoluteUri)" -ForegroundColor Green

# 3. Submit Add Product Form
$productName = "Bose QuietComfort 45"
$addFormBody = "productName=" + [System.Uri]::EscapeDataString($productName) +
               "&productType=" + [System.Uri]::EscapeDataString("Headphones") +
               "&category=" + [System.Uri]::EscapeDataString("Electronics") +
               "&manufacturer=" + [System.Uri]::EscapeDataString("Bose") +
               "&image=" + [System.Uri]::EscapeDataString("https://images.unsplash.com/photo-1546435770-a3e426bf472b") +
               "&price=29990.00" +
               "&description=" + [System.Uri]::EscapeDataString("High fidelity wireless noise cancelling headphones") +
               "&specification=" + [System.Uri]::EscapeDataString("Battery: 24 hrs | USB-C Fast Charging")

$addPostRes = Invoke-WebRequest -Uri "$WebUrl/merchant/products/add" -Method Post -Body $addFormBody -ContentType "application/x-www-form-urlencoded" -WebSession $session -UseBasicParsing -MaximumRedirection 5
Write-Host "Product Add Response -> Redirected to $($addPostRes.BaseResponse.ResponseUri.AbsoluteUri)" -ForegroundColor Green

# 4. Verify in My Products List
$myProdsRes = Invoke-WebRequest -Uri "$WebUrl/merchant/products" -Method Get -WebSession $session -UseBasicParsing
$containsNewProduct = $myProdsRes.Content.Contains($productName)
Write-Host "My Products page contains '$productName': $containsNewProduct" -ForegroundColor Green

Write-Host "`n================================================================" -ForegroundColor Cyan
Write-Host "MERCHANT ADD PRODUCT WEB TEST COMPLETED!" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
