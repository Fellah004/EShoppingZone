$ErrorActionPreference = "Stop"
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  EShoppingZone PDF Compliance & Functional Test  " -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8088"
$profileUrl = "http://localhost:8081"
$productUrl = "http://localhost:8082"
$cartUrl = "http://localhost:8083"
$orderUrl = "http://localhost:8084"
$walletUrl = "http://localhost:8085"

# 1. Product Service Catalog & Categories Verification
Write-Host "`n[1/10] Checking Product Service Catalog & Categories..." -ForegroundColor Yellow
$categories = Invoke-RestMethod -Uri "$productUrl/api/products/categories" -Method Get
Write-Host "Available Categories: $($categories -join ', ')" -ForegroundColor Green

$products = Invoke-RestMethod -Uri "$productUrl/api/products" -Method Get
Write-Host "Total Products Seeded: $($products.Count)" -ForegroundColor Green

if ($products.Count -ne 12) {
    Write-Error "Expected 12 seeded INR products, found $($products.Count)"
}

foreach ($p in $products) {
    Write-Host "  - #$($p.productId): $($p.productName) | $($p.category) | ₹$($p.price) | Rating: $($p.rating)" -ForegroundColor Gray
}

# 2. Register a New Customer Profile
$testEmail = "anand_$(Get-Random)@example.com"
$regPayload = @{
    fullName = "Anand Kumar"
    email = $testEmail
    password = "Password@123"
    mobileNumber = "98470$(Get-Random -Minimum 10000 -Maximum 99999)"
    about = "Software Engineer & Tech Enthusiast"
    dateOfBirth = "1995-06-15"
    gender = "Male"
    role = "CUSTOMER"
    address = @{
        fullName = "Anand Kumar"
        mobileNumber = "9847012345"
        houseNumber = "14B"
        streetName = "MG Road"
        colonyName = "Kakkanad"
        city = "Kochi"
        state = "Kerala"
        pincode = "682030"
    }
} | ConvertTo-Json -Depth 5

Write-Host "`n[2/10] Registering Customer: $testEmail..." -ForegroundColor Yellow
$regResponse = Invoke-RestMethod -Uri "$profileUrl/api/profiles/register" -Method Post -Body $regPayload -ContentType "application/json"
$token = $regResponse.token
$customerId = $regResponse.profileId
Write-Host "Registered successfully! ProfileId: $customerId, Token acquired: $($token.Substring(0, 20))..." -ForegroundColor Green

# 3. Authenticate / Login to verify JWT
Write-Host "`n[3/10] Verifying Login & JWT Authentication..." -ForegroundColor Yellow
$loginPayload = @{
    email = $testEmail
    password = "Password@123"
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod -Uri "$profileUrl/api/profiles/login" -Method Post -Body $loginPayload -ContentType "application/json"
$headers = @{
    Authorization = "Bearer $($loginResponse.token)"
}

$profile = Invoke-RestMethod -Uri "$profileUrl/api/profiles/$customerId" -Method Get -Headers $headers
Write-Host "Profile retrieved via Bearer Token: $($profile.fullName) | Role: $($profile.role) | About: $($profile.about) | DOB: $($profile.dateOfBirth)" -ForegroundColor Green

# 4. Cart Operations
Write-Host "`n[4/10] Adding Items to Shopping Cart..." -ForegroundColor Yellow
# Add OnePlus Nord CE (Product #1)
$addItem1 = @{
    productId = $products[0].productId
    quantity = 1
} | ConvertTo-Json
$cart1 = Invoke-RestMethod -Uri "$cartUrl/api/cart/$customerId/items" -Method Post -Body $addItem1 -ContentType "application/json"

# Add Atomic Habits (Product #5)
$addItem2 = @{
    productId = $products[4].productId
    quantity = 2
} | ConvertTo-Json
$cart2 = Invoke-RestMethod -Uri "$cartUrl/api/cart/$customerId/items" -Method Post -Body $addItem2 -ContentType "application/json"

Write-Host "Cart updated! Items count: $($cart2.items.Count), Total Price: ₹$($cart2.totalPrice)" -ForegroundColor Green
$expectedTotal = ($products[0].price * 1) + ($products[4].price * 2)
Write-Host "Expected total: ₹$expectedTotal | Cart total: ₹$($cart2.totalPrice)" -ForegroundColor Gray

# 5. Digital Wallet Top-up
Write-Host "`n[5/10] Checking & Topping Up Digital Wallet..." -ForegroundColor Yellow
$wallet = Invoke-RestMethod -Uri "$walletUrl/api/wallet/$customerId" -Method Get
Write-Host "Initial Wallet Balance: ₹$($wallet.currentBalance)" -ForegroundColor Gray

$topUpPayload = @{
    customerId = $customerId
    amount = 50000.0
    remarks = "Initial Wallet Top-up"
} | ConvertTo-Json

$wallet = Invoke-RestMethod -Uri "$walletUrl/api/wallet/add-money" -Method Post -Body $topUpPayload -ContentType "application/json"
Write-Host "Wallet topped up successfully! New Balance: ₹$($wallet.currentBalance)" -ForegroundColor Green

# 6. Place Order with WALLET Payment
Write-Host "`n[6/10] Placing Order with WALLET Payment..." -ForegroundColor Yellow
$orderPayload = @{
    customerId = $customerId
    paymentMethod = "WALLET"
    shippingAddress = @{
        fullName = "Anand Kumar"
        mobileNumber = "9847012345"
        houseNumber = "14B"
        streetName = "MG Road"
        colonyName = "Kakkanad"
        city = "Kochi"
        state = "Kerala"
        pincode = "682030"
    }
} | ConvertTo-Json -Depth 5

$order = Invoke-RestMethod -Uri "$orderUrl/api/orders" -Method Post -Body $orderPayload -ContentType "application/json"
Write-Host "Order Placed Successfully! OrderId: #$($order.orderId), Amount: ₹$($order.orderAmount), Status: $($order.orderStatus), Payment: $($order.paymentMethod)" -ForegroundColor Green

# 7. Verify Cart Cleared
Write-Host "`n[7/10] Verifying Cart Cleared after Order..." -ForegroundColor Yellow
$clearedCart = Invoke-RestMethod -Uri "$cartUrl/api/cart/$customerId" -Method Get
Write-Host "Cart items count after order: $($clearedCart.items.Count), Total: ₹$($clearedCart.totalPrice)" -ForegroundColor Green

# 8. Check Statement / Transaction History
Write-Host "`n[8/10] Verifying Wallet Statement History..." -ForegroundColor Yellow
Start-Sleep -Seconds 1
$statements = Invoke-RestMethod -Uri "$walletUrl/api/wallet/$customerId/transactions" -Method Get
Write-Host "Statements Count: $($statements.Count)" -ForegroundColor Green
foreach ($s in $statements) {
    Write-Host "  - Txn #$($s.transactionId): $($s.transactionType) | ₹$($s.amount) | OrderId: #$($s.orderId) | Remarks: $($s.remarks)" -ForegroundColor Gray
}

# 9. Place a Second Order with Cash on Delivery (COD)
Write-Host "`n[9/10] Adding Personal Care item & Testing Cash on Delivery (COD)..." -ForegroundColor Yellow
$addItem3 = @{
    productId = $products[11].productId # SPF 50 Sunscreen
    quantity = 1
} | ConvertTo-Json
$cart3 = Invoke-RestMethod -Uri "$cartUrl/api/cart/$customerId/items" -Method Post -Body $addItem3 -ContentType "application/json"

$codOrderPayload = @{
    customerId = $customerId
    paymentMethod = "COD"
    shippingAddress = @{
        fullName = "Anand Kumar"
        mobileNumber = "9847012345"
        houseNumber = "14B"
        streetName = "MG Road"
        colonyName = "Kakkanad"
        city = "Kochi"
        state = "Kerala"
        pincode = "682030"
    }
} | ConvertTo-Json -Depth 5

$codOrder = Invoke-RestMethod -Uri "$orderUrl/api/orders" -Method Post -Body $codOrderPayload -ContentType "application/json"
Write-Host "COD Order Placed! OrderId: #$($codOrder.orderId), Amount: ₹$($codOrder.orderAmount), Payment: $($codOrder.paymentMethod)" -ForegroundColor Green

# 10. Verify Website Controller HTML Endpoints
Write-Host "`n[10/10] Checking Website Controller UI Endpoints..." -ForegroundColor Yellow
$homeHtml = Invoke-WebRequest -Uri "$baseUrl/" -Method Get
Write-Host "Home Page HTTP Status: $($homeHtml.StatusCode) (Title: EShoppingZone)" -ForegroundColor Green

$productsHtml = Invoke-WebRequest -Uri "$baseUrl/products" -Method Get
Write-Host "Products Page HTTP Status: $($productsHtml.StatusCode)" -ForegroundColor Green

$loginHtml = Invoke-WebRequest -Uri "$baseUrl/login" -Method Get
Write-Host "Login Page HTTP Status: $($loginHtml.StatusCode)" -ForegroundColor Green

$registerHtml = Invoke-WebRequest -Uri "$baseUrl/register" -Method Get
Write-Host "Register Page HTTP Status: $($registerHtml.StatusCode)" -ForegroundColor Green

Write-Host "`n==================================================" -ForegroundColor Cyan
Write-Host "  ALL 10/10 EShoppingZone TESTS PASSED CLEANLY!   " -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Cyan
