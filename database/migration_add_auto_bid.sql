-- =====================================================================
-- Migration: Add auto_bid column to bids table
-- Nếu bảng bids chưa có cột auto_bid, hãy chạy script này
-- =====================================================================

USE auction_db;

-- Kiểm tra và thêm cột auto_bid nếu chưa tồn tại
ALTER TABLE bids ADD COLUMN auto_bid TINYINT(1) NOT NULL DEFAULT 0 AFTER amount;

-- Verify
SELECT 'auto_bid column added successfully' AS status;
