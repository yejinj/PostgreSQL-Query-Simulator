-- 테스트 스키마 생성
CREATE SCHEMA IF NOT EXISTS test_ecommerce;
SET search_path = test_ecommerce;

-- 사용자 테이블
CREATE TABLE IF NOT EXISTS users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    phone VARCHAR(20),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 상품 카테고리 테이블
CREATE TABLE IF NOT EXISTS categories (
    category_id SERIAL PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL,
    parent_category_id INTEGER REFERENCES categories(category_id),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 상품 테이블
CREATE TABLE IF NOT EXISTS products (
    product_id SERIAL PRIMARY KEY,
    product_name VARCHAR(200) NOT NULL,
    category_id INTEGER REFERENCES categories(category_id),
    price DECIMAL(10,2) NOT NULL,
    cost DECIMAL(10,2) NOT NULL,
    stock_quantity INTEGER DEFAULT 0,
    description TEXT,
    sku VARCHAR(50) UNIQUE,
    weight DECIMAL(8,2),
    dimensions VARCHAR(50),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 주문 테이블
CREATE TABLE IF NOT EXISTS orders (
    order_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(user_id),
    order_status VARCHAR(20) DEFAULT 'pending',
    total_amount DECIMAL(12,2) NOT NULL,
    shipping_cost DECIMAL(8,2) DEFAULT 0,
    tax_amount DECIMAL(8,2) DEFAULT 0,
    discount_amount DECIMAL(8,2) DEFAULT 0,
    shipping_address TEXT,
    billing_address TEXT,
    payment_method VARCHAR(20),
    payment_status VARCHAR(20) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 주문 상품 테이블
CREATE TABLE IF NOT EXISTS order_items (
    order_item_id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES orders(order_id),
    product_id INTEGER NOT NULL REFERENCES products(product_id),
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 장바구니 테이블
CREATE TABLE IF NOT EXISTS cart_items (
    cart_item_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(user_id),
    product_id INTEGER NOT NULL REFERENCES products(product_id),
    quantity INTEGER NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, product_id)
);

-- 리뷰 테이블
CREATE TABLE IF NOT EXISTS reviews (
    review_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(user_id),
    product_id INTEGER NOT NULL REFERENCES products(product_id),
    order_id INTEGER REFERENCES orders(order_id),
    rating INTEGER CHECK (rating >= 1 AND rating <= 5),
    review_text TEXT,
    is_verified BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 재고 이력 테이블
CREATE TABLE IF NOT EXISTS inventory_logs (
    log_id SERIAL PRIMARY KEY,
    product_id INTEGER NOT NULL REFERENCES products(product_id),
    change_type VARCHAR(20) NOT NULL, -- 'purchase', 'sale', 'adjustment'
    quantity_change INTEGER NOT NULL,
    previous_quantity INTEGER,
    new_quantity INTEGER,
    reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_price ON products(price);
CREATE INDEX IF NOT EXISTS idx_products_sku ON products(sku);
CREATE INDEX IF NOT EXISTS idx_products_is_active ON products(is_active);

CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(order_status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);

CREATE INDEX IF NOT EXISTS idx_cart_items_user_id ON cart_items(user_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_product_id ON cart_items(product_id);

CREATE INDEX IF NOT EXISTS idx_reviews_product_id ON reviews(product_id);
CREATE INDEX IF NOT EXISTS idx_reviews_user_id ON reviews(user_id);
CREATE INDEX IF NOT EXISTS idx_reviews_rating ON reviews(rating);

CREATE INDEX IF NOT EXISTS idx_inventory_logs_product_id ON inventory_logs(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_logs_created_at ON inventory_logs(created_at);

-- 샘플 데이터 삽입
-- 카테고리 데이터
INSERT INTO categories (category_name, description) VALUES
('전자제품', '스마트폰, 노트북, 태블릿 등'),
('의류', '남성복, 여성복, 아동복'),
('도서', '소설, 전문서적, 만화'),
('스포츠', '운동용품, 스포츠웨어'),
('가정용품', '주방용품, 인테리어용품')
ON CONFLICT DO NOTHING;

INSERT INTO categories (category_name, parent_category_id, description) VALUES
('스마트폰', 1, '각종 스마트폰'),
('노트북', 1, '노트북 컴퓨터'),
('남성복', 2, '남성 의류'),
('여성복', 2, '여성 의류'),
('소설', 3, '문학 소설'),
('전문서적', 3, '기술, 학술 서적')
ON CONFLICT DO NOTHING;

-- 사용자 데이터 (10,000명)
INSERT INTO users (username, email, password_hash, first_name, last_name, phone, address)
SELECT 
    'user' || i,
    'user' || i || '@email.com',
    'hash' || i,
    '이름' || i,
    '성' || (i % 100),
    '010-' || LPAD((i % 10000)::text, 4, '0') || '-' || LPAD(((i * 7) % 10000)::text, 4, '0'),
    '서울시 강남구 테헤란로 ' || (i % 1000 + 1) || '번지'
FROM generate_series(1, 10000) AS i
ON CONFLICT DO NOTHING;

-- 상품 데이터 (5,000개)
INSERT INTO products (product_name, category_id, price, cost, stock_quantity, sku, description, weight, is_active)
SELECT 
    '상품 ' || i || ' - ' || 
    CASE (i % 6) + 1
        WHEN 1 THEN '스마트폰'
        WHEN 2 THEN '노트북'
        WHEN 3 THEN '의류'
        WHEN 4 THEN '도서'
        WHEN 5 THEN '스포츠용품'
        ELSE '가정용품'
    END,
    ((i % 6) + 1),
    (RANDOM() * 500000 + 10000)::DECIMAL(10,2),
    (RANDOM() * 300000 + 5000)::DECIMAL(10,2),
    (RANDOM() * 1000)::INTEGER,
    'SKU-' || LPAD(i::text, 6, '0'),
    '상품 ' || i || '의 상세 설명입니다. 품질 좋은 제품입니다.',
    (RANDOM() * 5 + 0.1)::DECIMAL(8,2),
    CASE WHEN i % 10 = 0 THEN false ELSE true END
FROM generate_series(1, 5000) AS i
ON CONFLICT DO NOTHING;

-- 주문 데이터 (50,000개)
INSERT INTO orders (user_id, order_status, total_amount, shipping_cost, tax_amount, discount_amount, shipping_address, payment_method, payment_status, created_at)
SELECT 
    (RANDOM() * 10000 + 1)::INTEGER,
    CASE (RANDOM() * 5)::INTEGER
        WHEN 0 THEN 'pending'
        WHEN 1 THEN 'confirmed'
        WHEN 2 THEN 'shipped'
        WHEN 3 THEN 'delivered'
        ELSE 'cancelled'
    END,
    (RANDOM() * 500000 + 1000)::DECIMAL(12,2),
    (RANDOM() * 5000 + 2500)::DECIMAL(8,2),
    (RANDOM() * 50000 + 100)::DECIMAL(8,2),
    (RANDOM() * 10000)::DECIMAL(8,2),
    '서울시 강남구 배송주소 ' || (RANDOM() * 1000 + 1)::INTEGER || '번지',
    CASE (RANDOM() * 3)::INTEGER
        WHEN 0 THEN 'card'
        WHEN 1 THEN 'bank'
        ELSE 'kakao'
    END,
    CASE (RANDOM() * 3)::INTEGER
        WHEN 0 THEN 'pending'
        WHEN 1 THEN 'completed'
        ELSE 'failed'
    END,
    CURRENT_TIMESTAMP - (RANDOM() * 365 || ' days')::INTERVAL
FROM generate_series(1, 50000) AS i;

-- 주문 상품 데이터 (150,000개, 평균 주문당 3개 상품)
INSERT INTO order_items (order_id, product_id, quantity, unit_price, total_price)
SELECT 
    (RANDOM() * 50000 + 1)::INTEGER,
    (RANDOM() * 5000 + 1)::INTEGER,
    (RANDOM() * 5 + 1)::INTEGER,
    (RANDOM() * 100000 + 1000)::DECIMAL(10,2),
    ((RANDOM() * 5 + 1) * (RANDOM() * 100000 + 1000))::DECIMAL(12,2)
FROM generate_series(1, 150000) AS i;

-- 리뷰 데이터 (30,000개)
INSERT INTO reviews (user_id, product_id, order_id, rating, review_text, is_verified, created_at)
SELECT 
    (RANDOM() * 10000 + 1)::INTEGER,
    (RANDOM() * 5000 + 1)::INTEGER,
    (RANDOM() * 50000 + 1)::INTEGER,
    (RANDOM() * 5 + 1)::INTEGER,
    '이 상품은 정말 좋습니다. 추천합니다. 리뷰 ' || i,
    RANDOM() > 0.3,
    CURRENT_TIMESTAMP - (RANDOM() * 200 || ' days')::INTERVAL
FROM generate_series(1, 30000) AS i;

-- 재고 이력 데이터 (100,000개)
INSERT INTO inventory_logs (product_id, change_type, quantity_change, previous_quantity, new_quantity, reason, created_at)
SELECT 
    (RANDOM() * 5000 + 1)::INTEGER,
    CASE (RANDOM() * 3)::INTEGER
        WHEN 0 THEN 'purchase'
        WHEN 1 THEN 'sale'
        ELSE 'adjustment'
    END,
    (RANDOM() * 100 - 50)::INTEGER,
    (RANDOM() * 1000)::INTEGER,
    (RANDOM() * 1000)::INTEGER,
    '재고 조정 사유 ' || i,
    CURRENT_TIMESTAMP - (RANDOM() * 365 || ' days')::INTERVAL
FROM generate_series(1, 100000) AS i;

-- 분석을 위한 뷰 생성
CREATE OR REPLACE VIEW sales_summary AS
SELECT 
    p.product_name,
    c.category_name,
    COUNT(oi.order_item_id) as total_sales,
    SUM(oi.quantity) as total_quantity,
    SUM(oi.total_price) as total_revenue,
    AVG(oi.unit_price) as avg_price
FROM products p
JOIN categories c ON p.category_id = c.category_id
LEFT JOIN order_items oi ON p.product_id = oi.product_id
GROUP BY p.product_id, p.product_name, c.category_name;

CREATE OR REPLACE VIEW user_order_stats AS
SELECT 
    u.user_id,
    u.username,
    u.email,
    COUNT(o.order_id) as total_orders,
    SUM(o.total_amount) as total_spent,
    AVG(o.total_amount) as avg_order_value,
    MAX(o.created_at) as last_order_date
FROM users u
LEFT JOIN orders o ON u.user_id = o.user_id
GROUP BY u.user_id, u.username, u.email;

-- 통계 정보 업데이트
ANALYZE;

-- 완료 메시지
DO $$
BEGIN
    RAISE NOTICE '=================================';
    RAISE NOTICE '테스트 데이터 생성 완료!';
    RAISE NOTICE '=================================';
    RAISE NOTICE '스키마: test_ecommerce';
    RAISE NOTICE '사용자: 10,000명';
    RAISE NOTICE '상품: 5,000개';
    RAISE NOTICE '주문: 50,000개';
    RAISE NOTICE '주문상품: 150,000개';
    RAISE NOTICE '리뷰: 30,000개';
    RAISE NOTICE '재고이력: 100,000개';
    RAISE NOTICE '=================================';
END $$; 