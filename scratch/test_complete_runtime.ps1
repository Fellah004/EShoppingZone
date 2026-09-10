$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

Write-Host "====================================================" -ForegroundColor Cyan
Write-Host " RUNNING COMPLETE ESHOPPINGZONE RUNTIME AUDIT SUITE " -ForegroundColor Cyan
Write-Host "====================================================" -ForegroundColor Cyan

# 1. Health checks
Write-Host "`n[1/6] Verifying All 6 Health Endpoints..." -ForegroundColor Yellow
$healthEndpoints = @(
    "http://localhost:8081/api/profile/health",
    "http://localhost:8082/api/products/health",
    "http://localhost:8083/api/cart/health",
    "http://localhost:8084/api/orders/health",
    "http://localhost:8085/api/wallet/health",
    "http://localhost:8088/health"
)
foreach ($url in $healthEndpoints) {
    $res = Invoke-RestMethod -Uri $url -Method Get -TimeoutSec 5
    Write-Host " -> $url : $($res | ConvertTo-Json -Compress)" -ForegroundColor Green
}

# 2. Verify Product Catalog & Local SVG Images
Write-Host "`n[2/6] Verifying Product Catalog & Local SVG Images..." -ForegroundColor Yellow
$products = Invoke-RestMethod -Uri "http://localhost:8082/api/products" -Method Get -TimeoutSec 5
Write-Host " -> Found $($products.Count) catalog products" -ForegroundColor Green
if ($products.Count -lt 12) {
    throw "Expected at least 12 products in catalog, found $($products.Count)"
}

# Check an SVG image retrieval through website-controller
$firstImg = $products[0].image
Write-Host " -> Fetching image '$firstImg' from website-controller..." -ForegroundColor Yellow
$imgRes = Invoke-WebRequest -Uri "http://localhost:8088$firstImg" -Method Get -UseBasicParsing -TimeoutSec 5
Write-Host " -> Image fetched successfully ($($imgRes.Content.Length) bytes, $($imgRes.Headers['Content-Type']))" -ForegroundColor Green

# 3. Register & Login Merchant (Profile ID 2 or New Merchant)
Write-Host "`n[3/6] Testing Merchant Profile & Product Management..." -ForegroundColor Yellow
$mEmail = "merchant_audit_$((Get-Date).Ticks)@eshoppingzone.com"
$mRegisterBody = @{
    fullName = "Apex Electronics Ltd"
    email = $mEmail
    password = "Password@123"
    mobileNumber = "9988$([math]::Round((Get-Random -Minimum 100000 -Maximum 999999)))"
    role = "MERCHANT"
} | ConvertTo-Json

$mAuth = Invoke-RestMethod -Uri "http://localhost:8081/api/profile/register" -Method Post -Body $mRegisterBody -ContentType "application/json" -TimeoutSec 5
$mToken = $mAuth.token
$mProfileId = $mAuth.profileId
Write-Host " -> Merchant registered: Profile ID $mProfileId ($mEmail)" -ForegroundColor Green

# Add Product as Merchant
$newProdBody = @{
    merchantId = $mProfileId
    productName = "Apex Studio Monitor"
    productType = "Speakers"
    category = "Electronics"
    manufacturer = "Apex Audio"
    image = "/images/products/jbl-headphones.svg"
    price = 8999.0
    description = "High-precision studio reference monitor."
    specification = "Power: 80W | Frequency: 45Hz-22kHz"
} | ConvertTo-Json

$mHeaders = @{ Authorization = "Bearer $mToken" }
$createdProd = Invoke-RestMethod -Uri "http://localhost:8082/api/products" -Method Post -Body $newProdBody -ContentType "application/json" -Headers $mHeaders -TimeoutSec 5
Write-Host " -> Merchant created product #$($createdProd.productId) ($($createdProd.productName)) with merchantId=$($createdProd.merchantId)" -ForegroundColor Green

# Verify Merchant's own product list
$mProducts = Invoke-RestMethod -Uri "http://localhost:8082/api/products/merchant/$mProfileId" -Method Get -TimeoutSec 5
Write-Host " -> Merchant owns $($mProducts.Count) products in their isolated view" -ForegroundColor Green
if ($mProducts.Count -ne 1) {
    throw "Expected 1 product for new merchant, found $($mProducts.Count)"
}

# 4. Customer Journey & Wallet Payment
Write-Host "`n[4/6] Testing Customer Order Journey with Wallet Payment..." -ForegroundColor Yellow
$cEmail = "cust_audit_$((Get-Date).Ticks)@eshoppingzone.com"
$cRegisterBody = @{
    fullName = "Rohan Verma"
    email = $cEmail
    password = "Password@123"
    mobileNumber = "9876$([math]::Round((Get-Random -Minimum 100000 -Maximum 999999)))"
    role = "CUSTOMER"
} | ConvertTo-Json

$cAuth = Invoke-RestMethod -Uri "http://localhost:8081/api/profile/register" -Method Post -Body $cRegisterBody -ContentType "application/json" -TimeoutSec 5
$cToken = $cAuth.token
$cProfileId = $cAuth.profileId
Write-Host " -> Customer registered: Profile ID $cProfileId ($cEmail)" -ForegroundColor Green

# Add Money to Wallet
$addMoneyBody = @{
    customerId = $cProfileId
    amount = 20000.0
    remarks = "Initial balance"
} | ConvertTo-Json
$wallet = Invoke-RestMethod -Uri "http://localhost:8085/api/wallet/add-money" -Method Post -Body $addMoneyBody -ContentType "application/json" -TimeoutSec 5
Write-Host " -> Added funds to wallet. Balance: ₹$($wallet.currentBalance)" -ForegroundColor Green

# Add Merchant's Product to Cart
$addToCartBody = @{
    productId = $createdProd.productId
    quantity = 2
} | ConvertTo-Json
$cart = Invoke-RestMethod -Uri "http://localhost:8083/api/cart/$cProfileId/items" -Method Post -Body $addToCartBody -ContentType "application/json" -TimeoutSec 5
Write-Host " -> Added product to cart. Cart Total: ₹$($cart.totalPrice) (Items: $($cart.items.Count))" -ForegroundColor Green

# Place Order with WALLET
$placeOrderBody = @{
    customerId = $cProfileId
    paymentMethod = "WALLET"
    shippingAddress = @{
        fullName = "Rohan Verma"
        mobileNumber = "9876543210"
        houseNumber = "12B"
        streetName = "Park Street"
        colonyName = "Indiranagar"
        city = "Bengaluru"
        state = "Karnataka"
        pincode = "560038"
    }
} | ConvertTo-Json

$order = Invoke-RestMethod -Uri "http://localhost:8084/api/orders" -Method Post -Body $placeOrderBody -ContentType "application/json" -TimeoutSec 5
Write-Host " -> Order placed! Order ID: #$($order.orderId)" -ForegroundColor Green
Write-Host " -> Payment Method: $($order.paymentMethod) | Payment Status: $($order.paymentStatus)" -ForegroundColor Green
if ($order.paymentStatus -ne "PAID") {
    throw "Expected paymentStatus 'PAID' for Wallet purchase, got '$($order.paymentStatus)'"
}

# 5. Place COD Order to verify PENDING status
Write-Host "`n[5/6] Testing COD Order & Payment Status PENDING..." -ForegroundColor Yellow
$addToCartBody2 = @{
    productId = $products[0].productId
    quantity = 1
} | ConvertTo-Json
$null = Invoke-RestMethod -Uri "http://localhost:8083/api/cart/$cProfileId/items" -Method Post -Body $addToCartBody2 -ContentType "application/json" -TimeoutSec 5

$placeCodBody = @{
    customerId = $cProfileId
    paymentMethod = "COD"
    shippingAddress = @{
        fullName = "Rohan Verma"
        mobileNumber = "9876543210"
        houseNumber = "12B"
        streetName = "Park Street"
        city = "Bengaluru"
        state = "Karnataka"
        pincode = "560038"
    }
} | ConvertTo-Json
$codOrder = Invoke-RestMethod -Uri "http://localhost:8084/api/orders" -Method Post -Body $placeCodBody -ContentType "application/json" -TimeoutSec 5
Write-Host " -> COD Order placed! Order ID: #$($codOrder.orderId) | Payment Status: $($codOrder.paymentStatus)" -ForegroundColor Green
if ($codOrder.paymentStatus -ne "PENDING") {
    throw "Expected paymentStatus 'PENDING' for COD purchase, got '$($codOrder.paymentStatus)'"
}

# 6. Verify Merchant Sales Visibility
Write-Host "`n[6/6] Testing Merchant Sales Visibility for Merchant #$mProfileId..." -ForegroundColor Yellow
$sales = Invoke-RestMethod -Uri "http://localhost:8084/api/orders/merchant/$mProfileId" -Method Get -TimeoutSec 5
Write-Host " -> Retrieved $($sales.Count) sales line items for Merchant #$mProfileId" -ForegroundColor Green
if ($sales.Count -lt 1) {
    throw "Expected at least 1 sale item for merchant $mProfileId, found 0"
}
$sale = $sales[0]
Write-Host " -> Sale Line Item: Order #$($sale.orderId) | Product: $($sale.productName) | Qty: $($sale.quantity) | SubTotal: ₹$($sale.subTotal) | Customer: $($sale.customerName) | Payment: $($sale.paymentMethod) ($($sale.paymentStatus))" -ForegroundColor Cyan

Write-Host "`n====================================================" -ForegroundColor Green
Write-Host " ALL AUDIT AND INTEGRATION TESTS PASSED CLEANLY!    " -ForegroundColor Green
Write-Host "====================================================" -ForegroundColor Green
