CREATE DATABASE gudos_nest;
USE gudos_nest;

CREATE TABLE Users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(60) NOT NULL,
    is_owner BOOLEAN NOT NULL DEFAULT FALSE,
    is_renter BOOLEAN NOT NULL DEFAULT TRUE,
    account_status ENUM('active','suspended')
        NOT NULL DEFAULT 'active',
    CHECK (is_owner = TRUE OR is_renter = TRUE)
);

CREATE TABLE Properties (
    property_id INT AUTO_INCREMENT PRIMARY KEY,
    owner_id INT NOT NULL,
    property_name VARCHAR(100) NOT NULL,
    location VARCHAR(250) NOT NULL,
    acquisition_cost DECIMAL(12,2) NOT NULL CHECK (acquisition_cost >= 0),
    acquisition_date DATE,
    FOREIGN KEY (owner_id) REFERENCES Users(user_id)
);

CREATE TABLE Units (
    unit_id INT AUTO_INCREMENT PRIMARY KEY,
    property_id INT NOT NULL,
    unit_name VARCHAR(100) NOT NULL,
    unit_description TEXT NULL,
    capacity INT NOT NULL CHECK (capacity > 0),
    daily_rate DECIMAL(10,2) NOT NULL CHECK (daily_rate > 0),
    unit_status ENUM('unoccupied','occupied','maintenance') NOT NULL DEFAULT 'unoccupied',
    FOREIGN KEY (property_id) REFERENCES Properties(property_id)
);

CREATE TABLE Bookings (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    renter_id INT NOT NULL,
    unit_id INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    check_in DATETIME NOT NULL,
    check_out DATETIME NOT NULL,
    number_of_renters INT NOT NULL CHECK (number_of_renters > 0),
    special_requests TEXT NULL,
    booking_status ENUM('pending','approved','rejected','cancelled','completed') NOT NULL DEFAULT 'pending',
    total_amount DECIMAL(10,2) NOT NULL CHECK (total_amount >= 0),
    cancelled_at DATETIME NULL,
    CHECK (check_out > check_in),
    CHECK (booking_status <> 'cancelled' OR cancelled_at IS NOT NULL),
    FOREIGN KEY (renter_id) REFERENCES Users(user_id),
    FOREIGN KEY (unit_id) REFERENCES Units(unit_id)
);

CREATE TABLE Payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    receipt_url TEXT NULL,
    payment_type ENUM('rental_payment', 'damage_fee') NOT NULL,
    payment_status ENUM('pending','paid') NOT NULL DEFAULT 'pending',
    payment_date DATETIME NULL,
    CHECK (payment_status = 'pending' OR (payment_date IS NOT NULL AND receipt_url IS NOT NULL)),
    FOREIGN KEY (booking_id) REFERENCES Bookings(booking_id)
);

CREATE TABLE Refunds (
    refund_id INT AUTO_INCREMENT PRIMARY KEY,
    payment_id INT NOT NULL,
    cancellation_fee DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (cancellation_fee >= 0),
    cancellation_reason TEXT NULL,
    refund_status ENUM('requested','approved','rejected','processed') NOT NULL DEFAULT 'requested',
    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME NULL,
    refund_receipt_url TEXT NULL,
    CHECK (refund_status <> 'processed' OR (processed_at IS NOT NULL AND refund_receipt_url IS NOT NULL)),
    FOREIGN KEY (payment_id) REFERENCES Payments(payment_id)
);

CREATE TABLE FinancialTransactions (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    property_id INT NOT NULL,
    payment_id INT NULL,
    transaction_type ENUM('rental_income','damage_fee_income','refund','maintenance_expense','supply_expense') NOT NULL,
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    transaction_date DATETIME NOT NULL,
    CHECK (transaction_type NOT IN ('rental_income','refund', 'damage_fee_income') OR payment_id IS NOT NULL),
    FOREIGN KEY (property_id) REFERENCES Properties(property_id),
    FOREIGN KEY (payment_id) REFERENCES Payments(payment_id)
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
    FOREIGN KEY (booking_id) REFERENCES Bookings(booking_id)
);

CREATE TABLE Items (
    item_id INT AUTO_INCREMENT PRIMARY KEY,
    property_id INT NOT NULL,
    unit_id INT NULL,
    item_name VARCHAR(100) NOT NULL,
    item_type ENUM('consumable','equipment') NOT NULL,
    quantity INT NOT NULL CHECK (quantity >= 0),
    unit_price DECIMAL(10,2) NOT NULL CHECK (unit_price >= 0),
    item_condition ENUM('good','damaged','maintenance','missing') NOT NULL DEFAULT 'good',
    FOREIGN KEY (property_id) REFERENCES Properties(property_id),
    FOREIGN KEY (unit_id) REFERENCES Units(unit_id)
);

CREATE TABLE Maintenance (
    maintenance_id INT AUTO_INCREMENT PRIMARY KEY,
    property_id INT NOT NULL,
    unit_id INT NULL,
    item_id INT NULL,
    maintenance_description TEXT NOT NULL,
    maintenance_cost DECIMAL(10,2) NOT NULL CHECK (maintenance_cost >= 0),
    maintenance_status ENUM('pending','in_progress','completed') NOT NULL DEFAULT 'pending',
    started_at DATETIME NOT NULL,
    completed_at DATETIME NULL,
    CHECK (completed_at IS NULL OR completed_at >= started_at),
    FOREIGN KEY (property_id) REFERENCES Properties(property_id),
    FOREIGN KEY (unit_id) REFERENCES Units(unit_id),
    FOREIGN KEY (item_id) REFERENCES Items(item_id)
);
