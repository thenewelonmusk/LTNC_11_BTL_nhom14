-- =====================================================================
--  AUCTION SYSTEM – Database Schema (MySQL 8.0+)
--  Nhóm 14 – Lập trình nâng cao
--  File: schema.sql
--
--  Mục đích: Tạo CSDL `auction_db` cùng toàn bộ bảng cần thiết cho
--  hệ thống đấu giá trực tuyến (users, items, auctions, bids).
--
--  Cách chạy:
--      mysql -u root -p < schema.sql
--  hoặc trong MySQL Workbench: mở file -> Execute (Ctrl+Shift+Enter)
-- =====================================================================

-- ---------- 1. Tạo database ----------
DROP DATABASE IF EXISTS auction_db;
CREATE DATABASE auction_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE auction_db;

-- =====================================================================
--  Bảng USERS — lưu tài khoản người dùng (BIDDER / SELLER / ADMIN)
-- =====================================================================
CREATE TABLE users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(64)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        ENUM('BIDDER', 'SELLER', 'ADMIN') NOT NULL DEFAULT 'BIDDER',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_users_role (role)
) ENGINE = InnoDB;

-- =====================================================================
--  Bảng ITEMS — sản phẩm do SELLER đăng lên
--  Các cột phải khớp đúng với ItemDAO.java:
--    id, name, description, category, starting_price, current_price,
--    seller_id, status
-- =====================================================================
CREATE TABLE items (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    category        VARCHAR(50)  NOT NULL,           -- VEHICLE | ELECTRONICS | ART | ...
    starting_price  DECIMAL(18,2) NOT NULL DEFAULT 0,
    current_price   DECIMAL(18,2) NOT NULL DEFAULT 0,
    seller_id       BIGINT       NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN',  -- OPEN | RUNNING | CLOSED | SOLD
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_items_seller   (seller_id),
    INDEX idx_items_category (category),
    INDEX idx_items_status   (status),
    CONSTRAINT fk_items_seller
        FOREIGN KEY (seller_id) REFERENCES users(id)
        ON DELETE CASCADE
) ENGINE = InnoDB;

-- =====================================================================
--  Bảng AUCTIONS — các phiên đấu giá
--  Khớp với AuctionDAO.java:
--    id, item_id, seller_id, start_price, current_price, winner_id,
--    start_time, end_time, status
--  Trạng thái khớp enum AuctionStatus.java: OPEN, RUNNING, FINISHED, PAID, CANCELED
-- =====================================================================
CREATE TABLE auctions (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    item_id       BIGINT        NOT NULL,
    seller_id     BIGINT        NOT NULL,
    start_price   DECIMAL(18,2) NOT NULL DEFAULT 0,
    current_price DECIMAL(18,2) NOT NULL DEFAULT 0,
    winner_id     BIGINT        DEFAULT NULL,
    start_time    DATETIME      NOT NULL,
    end_time      DATETIME      NOT NULL,
    status        ENUM('OPEN','RUNNING','FINISHED','PAID','CANCELED') NOT NULL DEFAULT 'OPEN',
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_auctions_item   (item_id),
    INDEX idx_auctions_seller (seller_id),
    INDEX idx_auctions_status (status),
    INDEX idx_auctions_winner (winner_id),
    CONSTRAINT fk_auctions_item
        FOREIGN KEY (item_id) REFERENCES items(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_auctions_seller
        FOREIGN KEY (seller_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_auctions_winner
        FOREIGN KEY (winner_id) REFERENCES users(id)
        ON DELETE SET NULL
) ENGINE = InnoDB;

-- =====================================================================
--  Bảng BIDS — lịch sử đặt giá thầu
--  Khớp với BidDAO.java:
--    id, auction_id, bidder_id, amount, (created_at)
-- =====================================================================
CREATE TABLE bids (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    auction_id  BIGINT        NOT NULL,
    bidder_id   BIGINT        NOT NULL,
    amount      DECIMAL(18,2) NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_bids_auction (auction_id),
    INDEX idx_bids_bidder  (bidder_id),
    INDEX idx_bids_amount  (auction_id, amount DESC),
    CONSTRAINT fk_bids_auction
        FOREIGN KEY (auction_id) REFERENCES auctions(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_bids_bidder
        FOREIGN KEY (bidder_id) REFERENCES users(id)
        ON DELETE CASCADE
) ENGINE = InnoDB;

-- =====================================================================
--  DỮ LIỆU MẪU (seed) — bỏ block này nếu không cần
-- =====================================================================

-- ---- Users mẫu (mật khẩu lưu plain-text để khớp với UserDAO hiện tại) ----
INSERT INTO users (username, password, role) VALUES
    ('admin',   'admin123',  'ADMIN'),
    ('seller1', 'seller123', 'SELLER'),
    ('seller2', 'seller123', 'SELLER'),
    ('bidder1', 'bidder123', 'BIDDER'),
    ('bidder2', 'bidder123', 'BIDDER'),
    ('bidder3', 'bidder123', 'BIDDER');

-- ---- Items mẫu ----
INSERT INTO items (name, description, category, starting_price, current_price, seller_id, status) VALUES
    ('iPhone 15 Pro Max',        'May nguyen seal, bao hanh 12 thang',          'ELECTRONICS', 25000000, 25000000, 2, 'RUNNING'),
    ('Honda SH 150i 2022',       'Xe di 5000km, bien HN, day du giay to',       'VEHICLE',     85000000, 85000000, 2, 'RUNNING'),
    ('Tranh son dau Phong canh', 'Tranh ve tay kho 80x120cm',                   'ART',          5000000,  5000000, 3, 'OPEN'),
    ('MacBook Pro M3',           'Phien ban 16-inch, RAM 32GB, SSD 1TB',         'ELECTRONICS', 55000000, 55000000, 3, 'OPEN');

-- ---- Auctions mẫu ----
INSERT INTO auctions (item_id, seller_id, start_price, current_price, start_time, end_time, status) VALUES
    (1, 2, 25000000, 25500000, NOW() - INTERVAL 1 HOUR,    NOW() + INTERVAL 1 DAY, 'RUNNING'),
    (2, 2, 85000000, 85000000, NOW() - INTERVAL 30 MINUTE, NOW() + INTERVAL 2 DAY, 'RUNNING'),
    (3, 3,  5000000,  5000000, NOW() + INTERVAL 1 HOUR,    NOW() + INTERVAL 3 DAY, 'OPEN'),
    (4, 3, 55000000, 55000000, NOW() + INTERVAL 2 HOUR,    NOW() + INTERVAL 5 DAY, 'OPEN');

-- ---- Bids mẫu cho phiên đầu tiên (để chart có dữ liệu để vẽ) ----
INSERT INTO bids (auction_id, bidder_id, amount) VALUES
    (1, 4, 25100000),
    (1, 5, 25200000),
    (1, 6, 25350000),
    (1, 4, 25500000);

-- =====================================================================
--  KIỂM TRA NHANH
-- =====================================================================
SELECT 'Tong so users'    AS info, COUNT(*) AS total FROM users
UNION ALL
SELECT 'Tong so items',    COUNT(*) FROM items
UNION ALL
SELECT 'Tong so auctions', COUNT(*) FROM auctions
UNION ALL
SELECT 'Tong so bids',     COUNT(*) FROM bids;
