package com.eshoppingzone.product.config;

import com.eshoppingzone.product.entity.Product;
import com.eshoppingzone.product.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;

    public DataInitializer(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        productRepository.deleteAll();

        Long defaultMerchantId = 2L;

        List<Product> initialProducts = List.of(
            // Electronics
            new Product(null, defaultMerchantId, "Smartphone", "OnePlus Nord CE Smartphone", "Electronics", "OnePlus",
                "/images/products/oneplus-smartphone.jpg",
                24999.0,
                "OnePlus Nord CE features a sleek design, 64MP triple camera system, Warp Charge 30T Plus, and a 90Hz Fluid AMOLED display.",
                "Display: 6.43-inch 90Hz Fluid AMOLED | Processor: Snapdragon 750G 5G | RAM: 8GB | Storage: 128GB | Battery: 4500mAh | OS: OxygenOS",
                4.5, "Excellent performance and battery life"),

            new Product(null, defaultMerchantId, "Headphones", "JBL Wireless Headphones", "Electronics", "JBL",
                "/images/products/jbl-headphones.jpg",
                4999.0,
                "JBL Pure Bass sound with active noise cancellation, wireless Bluetooth streaming, and up to 44 hours of battery life.",
                "Battery: Up to 44 hours | Driver: 32mm Dynamic | Bluetooth: 5.0 | Charging: USB Type-C fast charge | Weight: 220g",
                4.3, "Deep bass and comfortable ear cushions"),

            new Product(null, defaultMerchantId, "Smartwatch", "Noise Smart Watch", "Electronics", "Noise",
                "/images/products/noise-smartwatch.jpg",
                3499.0,
                "Noise ColorFit smart watch with 1.8-inch HD display, Bluetooth calling, 100+ sports modes, and 24/7 heart rate monitoring.",
                "Display: 1.8-inch TFT LCD | Battery: 7-day runtime | Water Resistance: IP68 | Sensors: SpO2, Heart Rate, Sleep Tracker",
                4.2, "Clear display and battery lasts full week"),

            // Books
            new Product(null, defaultMerchantId, "Fiction", "The Alchemist", "Books", "HarperCollins",
                "/images/products/the-alchemist.jpg",
                399.0,
                "Paulo Coelho's masterpiece tells the mystical story of Santiago, an Andalusian shepherd boy who yearns to travel in search of worldly treasure.",
                "Author: Paulo Coelho | Publisher: HarperCollins | Pages: 208 | Language: English | Format: Paperback | ISBN: 9780062315007",
                4.8, "Inspiring and timeless philosophical novel"),

            new Product(null, defaultMerchantId, "Self Help", "Atomic Habits", "Books", "Penguin Random House",
                "/images/products/atomic-habits.jpg",
                599.0,
                "An easy & proven way to build good habits and break bad ones. James Clear provides practical strategies that teach you exactly how to form good habits.",
                "Author: James Clear | Publisher: Penguin | Pages: 320 | Language: English | Format: Paperback | ISBN: 9781847941831",
                4.9, "Life-changing practical guide to habit formation"),

            new Product(null, defaultMerchantId, "Personal Development", "Ikigai", "Books", "Tuttle Publishing",
                "/images/products/ikigai.jpg",
                349.0,
                "The Japanese secret to a long and happy life. Discover the Japanese art of finding your purpose in life, staying active, and cultivating friendships.",
                "Authors: Hector Garcia, Francesc Miralles | Publisher: Penguin Life | Pages: 208 | Format: Hardcover | ISBN: 9781786330895",
                4.6, "Beautiful, insightful and calming read"),

            // Apparel
            new Product(null, defaultMerchantId, "Shirt", "Men's Casual Cotton Shirt", "Apparel", "Raymond",
                "/images/products/mens-cotton-shirt.jpg",
                1299.0,
                "Premium breathable 100% combed cotton casual button-down shirt. Tailored regular fit suitable for work and weekend outings.",
                "Fabric: 100% Pure Cotton | Fit: Regular Fit | Sleeve: Full Sleeves | Pattern: Solid / Oxford Weave | Collar: Spread Collar",
                4.4, "Great fabric quality and comfortable fit"),

            new Product(null, defaultMerchantId, "Kurti", "Women's Cotton Kurti", "Apparel", "FabIndia",
                "/images/products/womens-cotton-kurti.jpg",
                1499.0,
                "Handcrafted pure cotton straight-fit Kurti with elegant ethnic embroidery. Soft, lightweight, and perfect for daily and festive wear.",
                "Material: 100% Handloom Cotton | Style: Straight Kurti with Side Slits | Neck: Round Neck | Length: Calf Length",
                4.7, "Vibrant colors and very comfortable fabric"),

            new Product(null, defaultMerchantId, "Hoodie", "Unisex Casual Hoodie", "Apparel", "Roadster",
                "/images/products/unisex-casual-hoodie.jpg",
                1199.0,
                "Classic relaxed fit pullover hoodie crafted from heavyweight brushed fleece. Features a front kangaroo pocket and ribbed hem.",
                "Fabric: 80% Cotton, 20% Polyester Fleece (320 GSM) | Fit: Relaxed Unisex | Hood: Drawstring Adjustable | Pocket: Kangaroo",
                4.5, "Warm and stylish for everyday winter wear"),

            // Personal Care
            new Product(null, defaultMerchantId, "Face Wash", "Daily Face Wash", "Personal Care", "Himalaya",
                "/images/products/daily-face-wash.jpg",
                299.0,
                "Gentle herbal daily face wash enriched with Neem and Turmeric to cleanse impurities, prevent pimples, and refresh skin.",
                "Key Ingredients: Neem, Turmeric | Skin Type: All Skin Types | Volume: 150ml | Paraben-Free: Yes | Dermatologically Tested",
                4.4, "Gentle on skin and clears blemishes"),

            new Product(null, defaultMerchantId, "Shampoo", "Herbal Shampoo", "Personal Care", "Khadi Naturals",
                "/images/products/herbal-shampoo.jpg",
                449.0,
                "Ayurvedic nourishing shampoo with Amla and Bhringraj extracts that strengthens hair roots, controls hair fall, and adds natural shine.",
                "Key Extracts: Amla, Bhringraj, Shikakai | Hair Type: Normal to Dry | Volume: 210ml | Sulfate-Free: Yes",
                4.5, "Leaves hair silky and reduces hair fall"),

            new Product(null, defaultMerchantId, "Sunscreen", "SPF 50 Sunscreen", "Personal Care", "Minimalist",
                "/images/products/spf-50-sunscreen.jpg",
                599.0,
                "Broad-spectrum SPF 50 PA++++ lightweight sunscreen formulated with multi-vitamins. Non-greasy, fast-absorbing with zero white cast.",
                "Protection: SPF 50 PA++++ Broad Spectrum | Key Actives: Niacinamide, Vitamin B5, Vitamin F | Volume: 50g | Finish: Dewy/Matte",
                4.8, "Lightweight, absorbs quickly without white cast")
        );

        productRepository.saveAll(initialProducts);
    }
}
