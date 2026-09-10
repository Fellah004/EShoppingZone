$ErrorActionPreference = "Continue"

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "EShoppingZone - Customer vs Merchant Security and Flow Tests" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan

$ProfileUrl = "http://localhost:8081"
$ProductUrl = "http://localhost:8082"
$CartUrl    = "http://localhost:8083"
$OrderUrl   = "http://localhost:8084"
$WalletUrl  = "http://localhost:8085"
$WebUrl     = "http://localhost:8088"

# 0. Get Existing Products
$products = Invoke-RestMethod -Uri "$ProductUrl/api/products" -Method Get
$firstProduct = $products[0]
$firstProductId = $firstProduct.productId
Write-Host "Found $($products.Count) products in catalog. Using sample Product #$firstProductId ($($firstProduct.productName))" -ForegroundColor Cyan

# 1. Register and Login Customer
Write-Host "`n--- Step 1: Register and Login Customer ---" -ForegroundColor Yellow
$CustRand = Get-Random -Minimum 1000 -Maximum 9999
$CustEmail = "customer_" + $CustRand + "@example.com"
$CustRegBody = @{
    fullName = "Rahul Sharma"
    email = $CustEmail
    mobileNumber = "9876" + (Get-Random -Minimum 100000 -Maximum 999999)
    password = "CustomerPassword123!"
    role = "CUSTOMER"
    gender = "Male"
    dateOfBirth = "1994-05-15"
    about = "Regular shopping customer from Bangalore"
    address = @{
        houseNumber = "Flat 402, Lotus Heights"
        streetName = "Indiranagar 100 Feet Road"
        colonyName = "Indiranagar"
        city = "Bengaluru"
        state = "Karnataka"
        pinCode = 560038
    }
} | ConvertTo-Json

$custRegRes = Invoke-RestMethod -Uri "$ProfileUrl/api/profile/register" -Method Post -Body $CustRegBody -ContentType "application/json"
$CustToken = $custRegRes.token
$CustId = $custRegRes.profileId
Write-Host "Customer registered successfully: ID=$CustId, Email=$CustEmail, Role=$($custRegRes.role)" -ForegroundColor Green

# 2. Register and Login Merchant
Write-Host "`n--- Step 2: Register and Login Merchant ---" -ForegroundColor Yellow
$MerchRand = Get-Random -Minimum 1000 -Maximum 9999
$MerchEmail = "merchant_" + $MerchRand + "@example.com"
$MerchRegBody = @{
    fullName = "Aarav Retailers"
    email = $MerchEmail
    mobileNumber = "9875" + (Get-Random -Minimum 100000 -Maximum 999999)
    password = "MerchantPassword123!"
    role = "MERCHANT"
    gender = "Male"
    dateOfBirth = "1988-08-20"
    about = "Authorized electronics and books merchant"
    address = @{
        houseNumber = "Shop 12, Commercial Complex"
        streetName = "Brigade Road"
        colonyName = "Central Business District"
        city = "Bengaluru"
        state = "Karnataka"
        pinCode = 560001
    }
} | ConvertTo-Json

$merchRegRes = Invoke-RestMethod -Uri "$ProfileUrl/api/profile/register" -Method Post -Body $MerchRegBody -ContentType "application/json"
$MerchToken = $merchRegRes.token
$MerchId = $merchRegRes.profileId
Write-Host "Merchant registered successfully: ID=$MerchId, Email=$MerchEmail, Role=$($merchRegRes.role)" -ForegroundColor Green

# 3. Security Tests on Product Service
Write-Host "`n--- Step 3: Security Authorization Tests on Product Service ---" -ForegroundColor Yellow

$sampleProduct = @{
    productName = "Sony Wireless ANC Headphones"
    productType = "Headphones"
    category = "Electronics"
    manufacturer = "Sony"
    image = "https://images.unsplash.com/photo-1546435770-a3e426bf472b"
    price = 19999.00
    description = "Industry leading noise cancellation with premium sound"
    specification = "Battery: 30 hours | Bluetooth: 5.2 | Weight: 250g"
} | ConvertTo-Json

function Test-ProtectedEndpoint($url, $method, $body, $token) {
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    try {
        if ($body) {
            $resp = Invoke-WebRequest -Uri $url -Method $method -Headers $headers -Body $body -ContentType "application/json" -UseBasicParsing
        } else {
            $resp = Invoke-WebRequest -Uri $url -Method $method -Headers $headers -UseBasicParsing
        }
        return [int]$resp.StatusCode
    } catch {
        if ($_.Exception.Response) {
            return [int]$_.Exception.Response.StatusCode
        }
        return 500
    }
}

# Test 1: Customer Product Creation (Expect 403)
Write-Host "`n[Security Test 1] Customer Product Creation Attempt (POST /api/products)..."
$status1 = Test-ProtectedEndpoint "$ProductUrl/api/products" "POST" $sampleProduct $CustToken
if ($status1 -eq 403) {
    Write-Host "PASS: Customer was correctly denied (HTTP 403 Forbidden)" -ForegroundColor Green
} else {
    Write-Host "FAIL: Expected 403, got $status1" -ForegroundColor Red
}

# Test 2: Customer Product Update (Expect 403)
Write-Host "`n[Security Test 2] Customer Product Update Attempt (PUT /api/products/$firstProductId)..."
$status2 = Test-ProtectedEndpoint "$ProductUrl/api/products/$firstProductId" "PUT" $sampleProduct $CustToken
if ($status2 -eq 403) {
    Write-Host "PASS: Customer was correctly denied (HTTP 403 Forbidden)" -ForegroundColor Green
} else {
    Write-Host "FAIL: Expected 403, got $status2" -ForegroundColor Red
}

# Test 3: Customer Product Delete (Expect 403)
Write-Host "`n[Security Test 3] Customer Product Delete Attempt (DELETE /api/products/$firstProductId)..."
$status3 = Test-ProtectedEndpoint "$ProductUrl/api/products/$firstProductId" "DELETE" $null $CustToken
if ($status3 -eq 403) {
    Write-Host "PASS: Customer was correctly denied (HTTP 403 Forbidden)" -ForegroundColor Green
} else {
    Write-Host "FAIL: Expected 403, got $status3" -ForegroundColor Red
}

# Test 4: Merchant Product Creation (Expect 201)
Write-Host "`n[Security Test 4] Merchant Product Creation (POST /api/products)..."
$createdProduct = $null
try {
    $createdProduct = Invoke-RestMethod -Uri "$ProductUrl/api/products" -Method Post -Body $sampleProduct -ContentType "application/json" -Headers @{ "Authorization" = "Bearer $MerchToken" }
    Write-Host "PASS: Merchant successfully created product: ID=$($createdProduct.productId), Name=$($createdProduct.productName), Price=INR $($createdProduct.price)" -ForegroundColor Green
} catch {
    Write-Host "FAIL: Merchant creation failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 5: Merchant Product Update (Expect 200)
Write-Host "`n[Security Test 5] Merchant Product Update (PUT /api/products/{id})..."
if ($createdProduct -ne $null) {
    $updateBody = @{
        productName = "Sony Wireless ANC Headphones - Updated Edition"
        productType = "Headphones"
        category = "Electronics"
        manufacturer = "Sony India"
        image = "https://images.unsplash.com/photo-1546435770-a3e426bf472b"
        price = 18999.00
        description = "Updated Edition with multipoint pairing"
        specification = "Battery: 35 hours | Bluetooth: 5.3"
    } | ConvertTo-Json

    try {
        $updatedProduct = Invoke-RestMethod -Uri "$ProductUrl/api/products/$($createdProduct.productId)" -Method Put -Body $updateBody -ContentType "application/json" -Headers @{ "Authorization" = "Bearer $MerchToken" }
        Write-Host "PASS: Merchant successfully updated product: Name=$($updatedProduct.productName), Price=INR $($updatedProduct.price)" -ForegroundColor Green
    } catch {
        Write-Host "FAIL: Merchant update failed: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Test 6: Merchant Product Delete (Expect 204)
Write-Host "`n[Security Test 6] Merchant Product Delete (DELETE /api/products/{id})..."
if ($createdProduct -ne $null) {
    $status6 = Test-ProtectedEndpoint "$ProductUrl/api/products/$($createdProduct.productId)" "DELETE" $null $MerchToken
    if ($status6 -eq 204 -or $status6 -eq 200) {
        Write-Host "PASS: Merchant successfully deleted product #$($createdProduct.productId) (HTTP $status6)" -ForegroundColor Green
    } else {
        Write-Host "FAIL: Expected 204 or 200, got $status6" -ForegroundColor Red
    }
}

# 4. Customer Full Shopping Flow
Write-Host "`n--- Step 4: Customer End-to-End Shopping Flow ---" -ForegroundColor Yellow

# Add to cart
$addBody = @{ productId = $firstProductId; quantity = 2 } | ConvertTo-Json
$cartRes = Invoke-RestMethod -Uri "$CartUrl/api/cart/$CustId/items" -Method Post -Body $addBody -ContentType "application/json"
Write-Host "Customer Cart: Total Items=$($cartRes.items.Count), Total Price=INR $($cartRes.totalPrice)" -ForegroundColor Green

# Add money to customer wallet
$topupBody = @{ customerId = $CustId; amount = 60000.00; remarks = "Customer salary credit" } | ConvertTo-Json
$walletRes = Invoke-RestMethod -Uri "$WalletUrl/api/wallet/add-money" -Method Post -Body $topupBody -ContentType "application/json"
Write-Host "Customer Wallet Balance: INR $($walletRes.currentBalance)" -ForegroundColor Green

# Place Order with Wallet Payment
$orderBody = @{
    customerId = $CustId
    paymentMethod = "WALLET"
    shippingAddress = @{
        fullName = "Rahul Sharma"
        mobileNumber = "9876543210"
        houseNumber = "Flat 402"
        streetName = "Indiranagar 100ft Rd"
        colonyName = "Indiranagar"
        city = "Bengaluru"
        state = "Karnataka"
        pinCode = 560038
    }
} | ConvertTo-Json

$orderRes = Invoke-RestMethod -Uri "$OrderUrl/api/orders" -Method Post -Body $orderBody -ContentType "application/json"
Write-Host "Order Placed Successfully! OrderId=$($orderRes.orderId), Status=$($orderRes.orderStatus), AmountPaid=INR $($orderRes.amountPaid)" -ForegroundColor Green

# Verify Cart was cleared
$clearedCart = Invoke-RestMethod -Uri "$CartUrl/api/cart/$CustId" -Method Get
Write-Host "Customer Cart after Order: Total Items=$($clearedCart.items.Count)" -ForegroundColor Green

# 5. Website Controller Frontend Pages Verification
Write-Host "`n--- Step 5: Website Controller UI Pages Verification ---" -ForegroundColor Yellow

$endpoints = @(
    "/",
    "/products",
    "/products/$firstProductId",
    "/products/search?query=OnePlus",
    "/products/category/Electronics",
    "/login",
    "/register"
)

foreach ($ep in $endpoints) {
    try {
        $page = Invoke-WebRequest -Uri "$WebUrl$ep" -Method Get -UseBasicParsing
        Write-Host "UI Page $ep -> HTTP $($page.StatusCode) (Length: $($page.Content.Length) bytes)" -ForegroundColor Green
    } catch {
        Write-Host "UI Page $ep -> FAILED: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n================================================================" -ForegroundColor Cyan
Write-Host "ALL SECURITY, ROLE, MERCHANT AND CUSTOMER TESTS COMPLETED!" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
