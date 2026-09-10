# EShoppingZone — Microservices E-Commerce Platform

EShoppingZone is a distributed e-commerce system built strictly in accordance with the official specification using **Java 21**, **Spring Boot 3**, **Spring Cloud OpenFeign**, **Spring AMQP (RabbitMQ)**, **MySQL 8.0**, and **Thymeleaf + Bootstrap 5**.

---

## 🏛 System Architecture & Microservices

The application consists of **6 targeted microservices** orchestrated via Docker Compose:

| Microservice | Port | Description | Database / Store |
|---|---|---|---|
| **Website Controller** | `8080` (mapped `8088`) | Presentation-layer Web MVC gateway with Warm-Orange/Terracotta theme, Merchant Dashboard & Customer shopping flows | Session Storage + JWT Propagator |
| **Profile Service** | `8081` | User registration, authentication, HMAC-SHA256 JWT generation, role management (`ROLE_CUSTOMER`, `ROLE_MERCHANT`), profile & address | MySQL (`eshopping_profile_db`) |
| **Product Service** | `8082` | Product catalog management, category filtering, search, Spring Security + JWT authorization (`ROLE_MERCHANT` only for POST/PUT/DELETE) | MySQL (`eshopping_product_db`) |
| **Cart Service** | `8083` | Customer shopping cart, item quantity updates, automatic price calculation (`sum(price * qty)`), OpenFeign client | MySQL (`eshopping_cart_db`) |
| **Order Service** | `8084` | Order placement, address capture, payment orchestration (Wallet / COD), AMQP event publisher | MySQL (`eshopping_order_db`) |
| **Wallet Service** | `8085` | Customer digital wallet, balance management, credit top-ups, debit payments, statement ledger, AMQP listener | MySQL (`eshopping_wallet_db`) |
| **RabbitMQ** | `5672`, `15672` | AMQP asynchronous event broker (`eshopping.order.exchange` $\rightarrow$ `eshopping.wallet.queue`) | In-Memory / Disk |
| **MySQL Database** | `3306` (mapped `3307`) | Relational persistence for all microservice databases | Persistent Volume |

---

## 👥 Role Separation: Customer vs Merchant

- **Customer (`ROLE_CUSTOMER`)**:
  - Buyer flow: Browse/search products $\rightarrow$ Add to Cart $\rightarrow$ Manage Cart $\rightarrow$ Checkout $\rightarrow$ Wallet / COD Payment $\rightarrow$ Place Order $\rightarrow$ Order History $\rightarrow$ Wallet Statements.
  - Customer is **blocked** (403 Forbidden) from creating, updating, or deleting catalog products, and from accessing Merchant pages.
- **Merchant (`ROLE_MERCHANT`)**:
  - Seller flow: Merchant Dashboard $\rightarrow$ View My Products $\rightarrow$ Add New Product in INR $\rightarrow$ Edit Product $\rightarrow$ Delete Product $\rightarrow$ Profile Management.
  - Merchant is **blocked** from customer cart, checkout, wallet purchasing, and order placement flows.

---

## 🔒 Security Matrix

| Function | Guest | Customer | Merchant |
|---|---|---|---|
| Browse Products | YES | YES | YES |
| Search Products | YES | YES | YES |
| View Product | YES | YES | YES |
| Add to Cart / Checkout | NO (Login) | YES | NO (Blocked) |
| Place Order / Wallet Payment | NO (Login) | YES | NO (Blocked) |
| Order History | NO (Login) | YES | NO (Blocked) |
| Add / Update / Delete Product | NO (401/403) | NO (403 Forbidden) | YES |
| Merchant Dashboard | NO (Login) | NO (Access Denied) | YES |

---

## 📦 Primary Categories & INR Pricing

1. **Electronics**: Smartphones, ANC Headphones, Smart Watches
2. **Books**: The Alchemist, Atomic Habits, Ikigai
3. **Apparel**: Men's Casual Cotton Shirt, Women's Cotton Kurti, Unisex Casual Hoodie
4. **Personal Care**: Daily Face Wash, Herbal Shampoo, SPF 50 Sunscreen

---

## 🎨 Theme & UI Design

- **Primary Color**: `#D9825B` (Warm Orange / Terracotta)
- **Primary Hover**: `#C66F49`
- **Primary Light**: `#FFF4EE`
- **Background**: `#FAF8F6`
- **Text & Dark Accents**: `#2D2926`
- **Border**: `#E8E0DB`

---

## 🚀 Running the Application

1. Build all jars:
   ```bash
   mvn clean package -DskipTests
   ```

2. Start the entire ecosystem:
   ```bash
   docker compose up --build -d
   ```

3. Access the web application:
   - **Frontend UI**: [http://localhost:8088](http://localhost:8088)
   - **RabbitMQ Management Dashboard**: [http://localhost:15672](http://localhost:15672) (User: `guest` / Pass: `guest`)

---

## 🧪 Testing Role & Security Flows

Run the integration and security verification scripts:
```powershell
powershell -ExecutionPolicy Bypass -File "scratch/test_roles_flow.ps1"
powershell -ExecutionPolicy Bypass -File "scratch/test_merchant_web.ps1"
powershell -ExecutionPolicy Bypass -File "scratch/test_customer_web.ps1"
powershell -ExecutionPolicy Bypass -File "scratch/test_merchant_add_product_web.ps1"
```
