-- Initialize Logical Databases for EShoppingZone Microservices
CREATE DATABASE IF NOT EXISTS eshopping_profile_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS eshopping_product_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS eshopping_cart_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS eshopping_order_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS eshopping_wallet_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON eshopping_profile_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON eshopping_product_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON eshopping_cart_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON eshopping_order_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON eshopping_wallet_db.* TO 'root'@'%';
FLUSH PRIVILEGES;
