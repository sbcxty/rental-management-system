-- =====================================================================================
-- Gudo's Nest - database schema and seed data
--
-- Run the whole file in MySQL Workbench (or the mysql command line).
-- WARNING: it drops and re-creates the gudos_nest database, so all data is reset.
--
-- Seed dates are relative to the moment you run this file (NOW()), so every case
-- (pending with hours left, expired, check-in today, refund tiers...) works whenever
-- you test. All demo accounts use the password: password123
-- =====================================================================================

DROP DATABASE IF EXISTS gudos_nest;
CREATE DATABASE gudos_nest CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE gudos_nest;

-- -------------------------------------------------------------------------------------
-- Tables
-- -------------------------------------------------------------------------------------

CREATE TABLE Users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(60) NOT NULL,                      -- jBCrypt hash
    is_owner BOOLEAN NOT NULL DEFAULT FALSE,
    is_renter BOOLEAN NOT NULL DEFAULT TRUE,
    account_status ENUM('active','suspended') NOT NULL DEFAULT 'active',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (is_owner = TRUE OR is_renter = TRUE)
);

CREATE TABLE Properties (
    property_id INT AUTO_INCREMENT PRIMARY KEY,
    owner_id INT NOT NULL,
    property_name VARCHAR(100) NOT NULL,
    location VARCHAR(250) NOT NULL,
    property_type ENUM('house','multi_unit') NOT NULL,       -- a house gets exactly one unit
    acquisition_cost DECIMAL(12,2) NOT NULL CHECK (acquisition_cost >= 0),
    acquisition_date DATE NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES Users(user_id)
);

CREATE TABLE PropertyImages (
    image_id INT AUTO_INCREMENT PRIMARY KEY,
    property_id INT NOT NULL,
    image_path VARCHAR(255) NOT NULL,                        -- "res:..." bundled, else file path
    sort_order INT NOT NULL DEFAULT 0,
    FOREIGN KEY (property_id) REFERENCES Properties(property_id) ON DELETE CASCADE
);

CREATE TABLE Units (
    unit_id INT AUTO_INCREMENT PRIMARY KEY,
    property_id INT NOT NULL,
    unit_name VARCHAR(100) NOT NULL,
    unit_description TEXT NULL,
    capacity INT NOT NULL CHECK (capacity > 0),
    daily_rate DECIMAL(10,2) NOT NULL CHECK (daily_rate > 0),
    cleaning_fee DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (cleaning_fee >= 0),
    unit_status ENUM('unoccupied','occupied') NOT NULL DEFAULT 'unoccupied',
    FOREIGN KEY (property_id) REFERENCES Properties(property_id)
);

CREATE TABLE Bookings (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    renter_id INT NOT NULL,
    unit_id INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,                            -- created_at + 48 hours
    check_in DATETIME NOT NULL,
    check_out DATETIME NOT NULL,
    number_of_renters INT NOT NULL CHECK (number_of_renters > 0),
    special_requests TEXT NULL,
    booking_status ENUM('pending','approved','rejected','expired','cancelled','completed')
        NOT NULL DEFAULT 'pending',
    -- Price snapshot at booking time, so receipts stay correct if rates change later
    nightly_rate DECIMAL(10,2) NOT NULL CHECK (nightly_rate > 0),
    cleaning_fee DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (cleaning_fee >= 0),
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    total_amount DECIMAL(10,2) NOT NULL CHECK (total_amount >= 0),
    cancelled_at DATETIME NULL,
    cancelled_by ENUM('renter','owner') NULL,
    cancellation_reason TEXT NULL,
    checkout_confirmed_at DATETIME NULL,
    CHECK (check_out > check_in),
    CHECK (expires_at > created_at),
    CHECK (booking_status <> 'cancelled' OR (cancelled_at IS NOT NULL AND cancelled_by IS NOT NULL)),
    CHECK (booking_status <> 'completed' OR checkout_confirmed_at IS NOT NULL),
    FOREIGN KEY (renter_id) REFERENCES Users(user_id),
    FOREIGN KEY (unit_id) REFERENCES Units(unit_id),
    INDEX idx_bookings_unit (unit_id, booking_status, check_in),
    INDEX idx_bookings_renter (renter_id, booking_status)
) AUTO_INCREMENT = 8801;

CREATE TABLE Payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    receipt_url TEXT NULL,                                   -- path of the saved receipt image
    payment_type ENUM('rental_payment','damage_fee') NOT NULL,
    payment_status ENUM('pending','paid') NOT NULL DEFAULT 'pending',
    payment_date DATETIME NULL,
    CHECK (payment_status = 'pending' OR (payment_date IS NOT NULL AND receipt_url IS NOT NULL)),
    FOREIGN KEY (booking_id) REFERENCES Bookings(booking_id),
    INDEX idx_payments_booking (booking_id)
);

CREATE TABLE Refunds (
    refund_id INT AUTO_INCREMENT PRIMARY KEY,
    payment_id INT NOT NULL UNIQUE,                          -- at most one refund per payment
    policy_tier ENUM('full','partial','automatic') NOT NULL, -- automatic = no owner review
    cancellation_fee DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (cancellation_fee >= 0),
    refund_amount DECIMAL(10,2) NOT NULL CHECK (refund_amount >= 0),
    refund_status ENUM('requested','rejected','processed') NOT NULL DEFAULT 'requested',
    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at DATETIME NULL,
    processed_at DATETIME NULL,
    refund_receipt_url TEXT NULL,
    CHECK (refund_status <> 'processed' OR (processed_at IS NOT NULL AND refund_receipt_url IS NOT NULL)),
    CHECK (refund_status <> 'rejected' OR reviewed_at IS NOT NULL),
    FOREIGN KEY (payment_id) REFERENCES Payments(payment_id)
);

CREATE TABLE FinancialTransactions (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    property_id INT NOT NULL,
    payment_id INT NULL,
    transaction_type ENUM('rental_income','damage_fee_income','refund',
                          'maintenance_expense','supply_expense','utility_expense') NOT NULL,
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),        -- always positive; the type says + or -
    transaction_date DATETIME NOT NULL,
    note VARCHAR(255) NULL,
    CHECK (transaction_type NOT IN ('rental_income','refund','damage_fee_income') OR payment_id IS NOT NULL),
    FOREIGN KEY (property_id) REFERENCES Properties(property_id),
    FOREIGN KEY (payment_id) REFERENCES Payments(payment_id),
    INDEX idx_transactions_property_date (property_id, transaction_date)
);

CREATE TABLE Messages (
    message_id INT AUTO_INCREMENT PRIMARY KEY,
    sender_id INT NOT NULL,
    receiver_id INT NOT NULL,
    booking_id INT NULL,
    message_text TEXT NOT NULL,
    sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    CHECK (sender_id <> receiver_id),
    FOREIGN KEY (sender_id) REFERENCES Users(user_id),
    FOREIGN KEY (receiver_id) REFERENCES Users(user_id),
    FOREIGN KEY (booking_id) REFERENCES Bookings(booking_id),
    INDEX idx_messages_pair (sender_id, receiver_id, sent_at)
);

CREATE TABLE Items (
    item_id INT AUTO_INCREMENT PRIMARY KEY,
    property_id INT NOT NULL,
    unit_id INT NULL,                                        -- NULL = shared by the whole property
    item_name VARCHAR(100) NOT NULL,
    item_type ENUM('consumable','equipment') NOT NULL,
    quantity INT NOT NULL CHECK (quantity >= 0),
    unit_price DECIMAL(10,2) NOT NULL CHECK (unit_price >= 0),
    item_condition ENUM('good','damaged','maintenance','missing') NOT NULL DEFAULT 'good',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (property_id) REFERENCES Properties(property_id),
    FOREIGN KEY (unit_id) REFERENCES Units(unit_id)
);

CREATE TABLE DamageReports (
    report_id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT NOT NULL,                                 -- gives the renter, unit and property
    description TEXT NOT NULL,
    photo_path VARCHAR(255) NULL,
    requested_fee DECIMAL(10,2) NOT NULL CHECK (requested_fee > 0),
    report_status ENUM('open','charged','dismissed') NOT NULL DEFAULT 'open',
    payment_id INT NULL UNIQUE,                              -- the damage_fee payment once charged
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME NULL,
    CHECK (report_status <> 'charged' OR payment_id IS NOT NULL),
    CHECK (report_status = 'open' OR resolved_at IS NOT NULL),
    FOREIGN KEY (booking_id) REFERENCES Bookings(booking_id),
    FOREIGN KEY (payment_id) REFERENCES Payments(payment_id)
);
