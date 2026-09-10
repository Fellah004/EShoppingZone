$ErrorActionPreference = "Continue"

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "EShoppingZone - Customer Web Interface and Authorization Tests" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan

$WebUrl = "http://localhost:8088"
$ProfileUrl = "http://localhost:8081"

# 1. Register Customer
$CustEmail = "web_cust_" + (Get-Random -Minimum 1000 -Maximum 9999) + "@example.com"
$CustRegBody = @{
    fullName = "Ananya Sen"
    email = $CustEmail
    mobileNumber = "9873" + (Get-Random -Minimum 100000 -Maximum 999999)
    password = "CustomerPassword123!"
    role = "CUSTOMER"
    gender = "Female"
    dateOfBirth = "1996-07-22"
    about = "Online buyer from Kolkata"
    address = @{
        houseNumber = "Flat 12B"
        streetName = "Park Street"
        colonyName = "Central"
        city = "Kolkata"
        state = "West Bengal"
        pinCode = 700016
    }
} | ConvertTo-Json

$custRegRes = Invoke-RestMethod -Uri "$ProfileUrl/api/profile/register" -Method Post -Body $CustRegBody -ContentType "application/json"
Write-Host "Registered Customer: ID=$($custRegRes.profileId), Email=$CustEmail" -ForegroundColor Green

# 2. Login via Web Controller Form
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$escapedEmail = [System.Uri]::EscapeDataString($CustEmail)
$escapedPass  = [System.Uri]::EscapeDataString("CustomerPassword123!")
$loginBody = "email=$escapedEmail&password=$escapedPass"

$loginRes = Invoke-WebRequest -Uri "$WebUrl/login" -Method Post -Body $loginBody -ContentType "application/x-www-form-urlencoded" -WebSession $session -UseBasicParsing -MaximumRedirection 5
Write-Host "Web Login Redirect -> $($loginRes.BaseResponse.ResponseUri.AbsoluteUri)" -ForegroundColor Green

# 3. Access Shopping Cart
$cartRes = Invoke-WebRequest -Uri "$WebUrl/cart" -Method Get -WebSession $session -UseBasicParsing
Write-Host "Customer Cart -> HTTP $($cartRes.StatusCode) (Contains 'Shopping Cart': $($cartRes.Content.Contains('Shopping Cart')))" -ForegroundColor Green

# 4. Access Wallet
$walletRes = Invoke-WebRequest -Uri "$WebUrl/wallet" -Method Get -WebSession $session -UseBasicParsing
Write-Host "Customer Wallet -> HTTP $($walletRes.StatusCode) (Contains 'Digital Wallet': $($walletRes.Content.Contains('Digital Wallet')))" -ForegroundColor Green

# 5. Access Order History
$ordersRes = Invoke-WebRequest -Uri "$WebUrl/orders" -Method Get -WebSession $session -UseBasicParsing
Write-Host "Customer Orders -> HTTP $($ordersRes.StatusCode) (Contains 'Order History': $($ordersRes.Content.Contains('Order History')))" -ForegroundColor Green

# 6. Attempt Merchant Dashboard Access as Customer (Should be blocked/redirected to /)
$dashRes = Invoke-WebRequest -Uri "$WebUrl/merchant/dashboard" -Method Get -WebSession $session -UseBasicParsing -MaximumRedirection 5
Write-Host "Customer Merchant Dashboard Access -> Redirected to: $($dashRes.BaseResponse.ResponseUri.AbsoluteUri)" -ForegroundColor Green

Write-Host "`n================================================================" -ForegroundColor Cyan
Write-Host "CUSTOMER WEB INTERFACE TESTS COMPLETED!" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
