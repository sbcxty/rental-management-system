
-- -------------------------------------------------------------------------------------
-- Seed data
-- -------------------------------------------------------------------------------------
USE gudos_nest;

SET @now = NOW();
SET @d = CURDATE();
SET @pw = '$2a$10$6kxLt4t8KWa4Yb421ytUFOl99JWAsiBycxB0R7YWLbjci7mURN0z2';   -- password123

-- Users: Elena is owner AND renter (tests portal switching); Marco is owner only.
INSERT INTO Users (user_id, full_name, email, password_hash, is_owner, is_renter, created_at) VALUES
(1, 'Elena Vance',      'elena.vance@example.com',    @pw, TRUE,  TRUE,  @now - INTERVAL 400 DAY),
(2, 'Marco Dela Cruz',  'marco.delacruz@example.com', @pw, TRUE,  FALSE, @now - INTERVAL 380 DAY),
(3, 'Miguel Alvarez',   'miguel.alvarez@example.com', @pw, FALSE, TRUE,  @now - INTERVAL 200 DAY),
(4, 'Sofia Laurel',     'sofia.laurel@example.com',   @pw, FALSE, TRUE,  @now - INTERVAL 190 DAY),
(5, 'Camila Santos',    'camila.santos@example.com',  @pw, FALSE, TRUE,  @now - INTERVAL 150 DAY),
(6, 'David Kim',        'david.kim@example.com',      @pw, FALSE, TRUE,  @now - INTERVAL 120 DAY),
(7, 'Lucas Reyes',      'lucas.reyes@example.com',    @pw, FALSE, TRUE,  @now - INTERVAL 100 DAY),
(8, 'Ana Bautista',     'ana.bautista@example.com',   @pw, FALSE, TRUE,  @now - INTERVAL 60 DAY);

INSERT INTO Properties (property_id, owner_id, property_name, location, property_type, acquisition_cost, acquisition_date) VALUES
(1, 1, 'Highland Glass Cabin',      'Tagaytay, Cavite',     'house',      2800000.00, '2023-03-14'),
(2, 1, 'The Stone Courtyard Villa', 'Amadeo, Cavite',       'multi_unit', 6500000.00, '2022-08-01'),
(3, 1, 'Sanctuary Cove House',      'Nasugbu, Batangas',    'house',      4200000.00, '2023-11-20'),
(4, 1, 'Azure Coast Villas',        'El Nido, Palawan',     'multi_unit', 9000000.00, '2021-05-10'),
(5, 2, 'Pinecrest Forest Cottage',  'Baguio City, Benguet', 'house',      2100000.00, '2024-01-15'),
(6, 2, 'Terrace Garden Studio',     'Makati City, Metro Manila', 'house', 1900000.00, '2024-06-30');

INSERT INTO PropertyImages (property_id, image_path, sort_order) VALUES
(1, 'res:images/seed/highland-1.png', 1),  (1, 'res:images/seed/highland-2.png', 2),  (1, 'res:images/seed/highland-3.png', 3),
(2, 'res:images/seed/stone-1.png', 1),     (2, 'res:images/seed/stone-2.png', 2),     (2, 'res:images/seed/stone-3.png', 3),
(3, 'res:images/seed/sanctuary-1.png', 1), (3, 'res:images/seed/sanctuary-2.png', 2), (3, 'res:images/seed/sanctuary-3.png', 3),
(4, 'res:images/seed/azure-1.png', 1),     (4, 'res:images/seed/azure-2.png', 2),     (4, 'res:images/seed/azure-3.png', 3),
(5, 'res:images/seed/pinecrest-1.png', 1), (5, 'res:images/seed/pinecrest-2.png', 2), (5, 'res:images/seed/pinecrest-3.png', 3),
(6, 'res:images/seed/terrace-1.png', 1),   (6, 'res:images/seed/terrace-2.png', 2),   (6, 'res:images/seed/terrace-3.png', 3);

-- A house has one unit named "Whole house". Units 1 and 5 have renters staying now.
INSERT INTO Units (unit_id, property_id, unit_name, unit_description, capacity, daily_rate, cleaning_fee, unit_status) VALUES
(1,  1, 'Whole house',       'A small glass cabin on a quiet ridge, with a view of the hills.', 2, 5400.00,  800.00, 'occupied'),
(2,  2, 'Courtyard Suite A', 'Ground floor suite that opens to the courtyard and pool.',        2, 2800.00,  500.00, 'unoccupied'),
(3,  2, 'Courtyard Suite B', 'Second floor suite with a small kitchen.',                         3, 3400.00,  600.00, 'unoccupied'),
(4,  2, 'The Loft',          'Top floor loft with two queen beds and a private terrace.',        4, 4200.00,  800.00, 'unoccupied'),
(5,  3, 'Whole house',       'A stone house above a quiet cove, with steps down to the water.', 6, 8500.00, 1500.00, 'occupied'),
(6,  4, 'Room 1',            'Sea view room with one queen bed.',                                2, 6500.00,  900.00, 'unoccupied'),
(7,  4, 'Room 2',            'Garden room with one queen bed.',                                  2, 6500.00,  900.00, 'unoccupied'),
(8,  4, 'Room 3',            'Family room with two queen beds, facing the sea.',                 4, 9800.00, 1200.00, 'unoccupied'),
(9,  4, 'Room 4',            'Family room with two queen beds and a balcony.',                   4, 9800.00, 1200.00, 'unoccupied'),
(10, 5, 'Whole house',       'A wooden cottage among pine trees, with a fireplace.',            4, 4600.00,  700.00, 'unoccupied'),
(11, 6, 'Whole house',       'A bright studio with a planted terrace in the city.',             2, 3200.00,  500.00, 'unoccupied');

-- Bookings. Check-in is 2:00 PM, check-out 11:00 AM. total = nights x rate + cleaning - discount
-- (10% off the nightly subtotal for stays of 7 nights or more).
INSERT INTO Bookings (booking_id, renter_id, unit_id, created_at, expires_at, check_in, check_out, number_of_renters,
                      special_requests, booking_status, nightly_rate, cleaning_fee, discount_amount, total_amount,
                      cancelled_at, cancelled_by, cancellation_reason, checkout_confirmed_at) VALUES
-- Pending: waiting for the owner (8802 and 8803 overlap: approving one rejects the other)
(8801, 3, 1, @now - INTERVAL 2 HOUR,  @now + INTERVAL 46 HOUR, TIMESTAMP(@d + INTERVAL 12 DAY, '14:00'), TIMESTAMP(@d + INTERVAL 15 DAY, '11:00'), 2,
       'We will arrive around 6:00 PM.', 'pending', 5400, 800, 0, 17000, NULL, NULL, NULL, NULL),
(8802, 6, 3, @now - INTERVAL 30 HOUR, @now + INTERVAL 18 HOUR, TIMESTAMP(@d + INTERVAL 20 DAY, '14:00'), TIMESTAMP(@d + INTERVAL 24 DAY, '11:00'), 3,
       NULL, 'pending', 3400, 600, 0, 14200, NULL, NULL, NULL, NULL),
(8803, 7, 3, @now - INTERVAL 10 HOUR, @now + INTERVAL 38 HOUR, TIMESTAMP(@d + INTERVAL 21 DAY, '14:00'), TIMESTAMP(@d + INTERVAL 23 DAY, '11:00'), 2,
       NULL, 'pending', 3400, 600, 0, 7400, NULL, NULL, NULL, NULL),
-- Pending but its 48 hours already passed: the app expires and refunds it when it opens
(8804, 8, 5, @now - INTERVAL 50 HOUR, @now - INTERVAL 2 HOUR,  TIMESTAMP(@d + INTERVAL 30 DAY, '14:00'), TIMESTAMP(@d + INTERVAL 32 DAY, '11:00'), 5,
       NULL, 'pending', 8500, 1500, 0, 18500, NULL, NULL, NULL, NULL),
-- Expired and rejected (both refunded in full automatically)
(8805, 4, 7, @now - INTERVAL 5 DAY, @now - INTERVAL 3 DAY, TIMESTAMP(@d + INTERVAL 18 DAY, '14:00'), TIMESTAMP(@d + INTERVAL 20 DAY, '11:00'), 2,
       NULL, 'expired', 6500, 900, 0, 13900, NULL, NULL, NULL, NULL),
(8806, 5, 10, @now - INTERVAL 4 DAY, @now - INTERVAL 2 DAY, TIMESTAMP(@d + INTERVAL 9 DAY, '14:00'), TIMESTAMP(@d + INTERVAL 12 DAY, '11:00'), 2,
       NULL, 'rejected', 4600, 700, 0, 14500, NULL, NULL, NULL, NULL),
-- Approved, upcoming: 10 days away (full refund tier), 4 days (partial), 1 day (no refund)
(8807, 4, 2, @now - INTERVAL 6 DAY,  @now - INTERVAL 4 DAY,  TIMESTAMP(@d + INTERVAL 10 DAY, '14:00'), TIMESTAMP(@d + INTERVAL 13 DAY, '11:00'), 2,
       NULL, 'approved', 2800, 500, 0, 8900, NULL, NULL, NULL, NULL),
(8808, 3, 4, @now - INTERVAL 12 DAY, @now - INTERVAL 10 DAY, TIMESTAMP(@d + INTERVAL 4 DAY, '14:00'),  TIMESTAMP(@d + INTERVAL 8 DAY, '11:00'), 3,
       'Extra pillows, please.', 'approved', 4200, 800, 0, 17600, NULL, NULL, NULL, NULL),
(8809, 3, 6, @now - INTERVAL 9 DAY,  @now - INTERVAL 7 DAY,  TIMESTAMP(@d + INTERVAL 1 DAY, '14:00'),  TIMESTAMP(@d + INTERVAL 3 DAY, '11:00'), 2,
       NULL, 'approved', 6500, 900, 0, 13900, NULL, NULL, NULL, NULL),
-- Approved: staying now / checking in today / checking out today
(8810, 5, 1, @now - INTERVAL 20 DAY, @now - INTERVAL 18 DAY, TIMESTAMP(@d - INTERVAL 1 DAY, '14:00'),  TIMESTAMP(@d + INTERVAL 2 DAY, '11:00'), 2,
       NULL, 'approved', 5400, 800, 0, 17000, NULL, NULL, NULL, NULL),
(8811, 7, 8, @now - INTERVAL 14 DAY, @now - INTERVAL 12 DAY, TIMESTAMP(@d, '14:00'),                    TIMESTAMP(@d + INTERVAL 3 DAY, '11:00'), 4,
       NULL, 'approved', 9800, 1200, 0, 30600, NULL, NULL, NULL, NULL),
(8812, 8, 5, @now - INTERVAL 25 DAY, @now - INTERVAL 23 DAY, TIMESTAMP(@d - INTERVAL 3 DAY, '14:00'),  TIMESTAMP(@d, '11:00'), 4,
       NULL, 'approved', 8500, 1500, 0, 27000, NULL, NULL, NULL, NULL),
-- Completed
(8813, 4, 5, @now - INTERVAL 60 DAY, @now - INTERVAL 58 DAY, TIMESTAMP(@d - INTERVAL 40 DAY, '14:00'), TIMESTAMP(@d - INTERVAL 36 DAY, '11:00'), 4,
       NULL, 'completed', 8500, 1500, 0, 35500, NULL, NULL, NULL, TIMESTAMP(@d - INTERVAL 36 DAY, '10:30')),
(8814, 3, 10, @now - INTERVAL 75 DAY, @now - INTERVAL 73 DAY, TIMESTAMP(@d - INTERVAL 60 DAY, '14:00'), TIMESTAMP(@d - INTERVAL 57 DAY, '11:00'), 2,
       NULL, 'completed', 4600, 700, 0, 14500, NULL, NULL, NULL, TIMESTAMP(@d - INTERVAL 57 DAY, '10:45')),
(8815, 6, 1, @now - INTERVAL 35 DAY, @now - INTERVAL 33 DAY, TIMESTAMP(@d - INTERVAL 20 DAY, '14:00'), TIMESTAMP(@d - INTERVAL 17 DAY, '11:00'), 2,
       NULL, 'completed', 5400, 800, 0, 17000, NULL, NULL, NULL, TIMESTAMP(@d - INTERVAL 17 DAY, '10:50')),
-- Cancelled by the renter after approval: full refund (10 days before), partial (6 days, waiting
-- for review), partial (4 days, processed), no refund (1 day), full (26 days, waiting for review)
(8816, 5, 2, @now - INTERVAL 30 DAY, @now - INTERVAL 28 DAY, TIMESTAMP(@d - INTERVAL 5 DAY, '14:00'),  TIMESTAMP(@d - INTERVAL 2 DAY, '11:00'), 2,
       NULL, 'cancelled', 2800, 500, 0, 8900, @now - INTERVAL 15 DAY, 'renter', 'Change of travel plans.', NULL),
(8817, 7, 1, @now - INTERVAL 20 DAY, @now - INTERVAL 18 DAY, TIMESTAMP(@d + INTERVAL 5 DAY, '14:00'),  TIMESTAMP(@d + INTERVAL 8 DAY, '11:00'), 2,
       NULL, 'cancelled', 5400, 800, 0, 17000, @now - INTERVAL 1 DAY, 'renter', 'My work schedule changed.', NULL),
(8818, 8, 9, @now - INTERVAL 50 DAY, @now - INTERVAL 48 DAY, TIMESTAMP(@d - INTERVAL 30 DAY, '14:00'), TIMESTAMP(@d - INTERVAL 27 DAY, '11:00'), 3,
       NULL, 'cancelled', 9800, 1200, 0, 30600, @now - INTERVAL 34 DAY, 'renter', 'Family emergency.', NULL),
(8819, 6, 4, @now - INTERVAL 70 DAY, @now - INTERVAL 68 DAY, TIMESTAMP(@d - INTERVAL 50 DAY, '14:00'), TIMESTAMP(@d - INTERVAL 48 DAY, '11:00'), 2,
       NULL, 'cancelled', 4200, 800, 0, 9200, @now - INTERVAL 51 DAY, 'renter', 'Our flight was cancelled.', NULL),
-- Cancelled by the owner after approval: full refund, automatic
(8820, 4, 6, @now - INTERVAL 40 DAY, @now - INTERVAL 38 DAY, TIMESTAMP(@d - INTERVAL 12 DAY, '14:00'), TIMESTAMP(@d - INTERVAL 10 DAY, '11:00'), 2,
       NULL, 'cancelled', 6500, 900, 0, 13900, @now - INTERVAL 20 DAY, 'owner', 'The room needed repairs.', NULL),
-- Cancelled by the renter while still pending: full refund, automatic
(8821, 3, 5, @now - INTERVAL 10 DAY, @now - INTERVAL 8 DAY, TIMESTAMP(@d - INTERVAL 8 DAY, '14:00'),  TIMESTAMP(@d - INTERVAL 6 DAY, '11:00'), 2,
       NULL, 'cancelled', 8500, 1500, 0, 18500, @now - INTERVAL 9 DAY, 'renter', 'I booked the wrong dates.', NULL),
(8822, 8, 2, @now - INTERVAL 10 DAY, @now - INTERVAL 8 DAY, TIMESTAMP(@d + INTERVAL 25 DAY, '14:00'), TIMESTAMP(@d + INTERVAL 28 DAY, '11:00'), 2,
       NULL, 'cancelled', 2800, 500, 0, 8900, @now - INTERVAL 1 DAY, 'renter', 'Change of travel plans.', NULL),
-- Elena books as a renter at Marco's studio
(8823, 1, 11, @now - INTERVAL 3 DAY, @now - INTERVAL 1 DAY, TIMESTAMP(@d + INTERVAL 14 DAY, '14:00'), TIMESTAMP(@d + INTERVAL 16 DAY, '11:00'), 2,
       NULL, 'approved', 3200, 500, 0, 6900, NULL, NULL, NULL, NULL),
-- Older completed stays (fill the 6-month income chart). 8824 is 7 nights, so it gets the 10% discount.
(8824, 4, 1, @now - INTERVAL 170 DAY, @now - INTERVAL 168 DAY, TIMESTAMP(@d - INTERVAL 150 DAY, '14:00'), TIMESTAMP(@d - INTERVAL 143 DAY, '11:00'), 2,
       NULL, 'completed', 5400, 800, 3780, 34820, NULL, NULL, NULL, TIMESTAMP(@d - INTERVAL 143 DAY, '10:40')),
(8825, 5, 2, @now - INTERVAL 135 DAY, @now - INTERVAL 133 DAY, TIMESTAMP(@d - INTERVAL 120 DAY, '14:00'), TIMESTAMP(@d - INTERVAL 116 DAY, '11:00'), 2,
       NULL, 'completed', 2800, 500, 0, 11700, NULL, NULL, NULL, TIMESTAMP(@d - INTERVAL 116 DAY, '10:30')),
(8826, 6, 4, @now - INTERVAL 110 DAY, @now - INTERVAL 108 DAY, TIMESTAMP(@d - INTERVAL 95 DAY, '14:00'), TIMESTAMP(@d - INTERVAL 92 DAY, '11:00'), 3,
       NULL, 'completed', 4200, 800, 0, 13400, NULL, NULL, NULL, TIMESTAMP(@d - INTERVAL 92 DAY, '10:55')),
(8827, 7, 5, @now - INTERVAL 90 DAY, @now - INTERVAL 88 DAY, TIMESTAMP(@d - INTERVAL 75 DAY, '14:00'), TIMESTAMP(@d - INTERVAL 72 DAY, '11:00'), 5,
       NULL, 'completed', 8500, 1500, 0, 27000, NULL, NULL, NULL, TIMESTAMP(@d - INTERVAL 72 DAY, '10:20')),
(8828, 3, 9, @now - INTERVAL 60 DAY, @now - INTERVAL 58 DAY, TIMESTAMP(@d - INTERVAL 45 DAY, '14:00'), TIMESTAMP(@d - INTERVAL 42 DAY, '11:00'), 3,
       NULL, 'completed', 9800, 1200, 0, 30600, NULL, NULL, NULL, TIMESTAMP(@d - INTERVAL 42 DAY, '10:35')),
(8829, 7, 11, @now - INTERVAL 40 DAY, @now - INTERVAL 38 DAY, TIMESTAMP(@d - INTERVAL 25 DAY, '14:00'), TIMESTAMP(@d - INTERVAL 23 DAY, '11:00'), 2,
       NULL, 'completed', 3200, 500, 0, 6900, NULL, NULL, NULL, TIMESTAMP(@d - INTERVAL 23 DAY, '10:50'));

-- One rental payment per booking, paid when booked (pay first, owner approves after).
INSERT INTO Payments (payment_id, booking_id, amount, receipt_url, payment_type, payment_status, payment_date)
SELECT booking_id - 8800, booking_id, total_amount,
       CONCAT('receipts/GN-', YEAR(created_at), '-', booking_id, '.png'),
       'rental_payment', 'paid', created_at
FROM Bookings;

-- Damage fee charged to David for booking 8815 (see DamageReports).
INSERT INTO Payments (payment_id, booking_id, amount, receipt_url, payment_type, payment_status, payment_date) VALUES
(30, 8815, 1200.00, 'receipts/damage-fee-1.png', 'damage_fee', 'paid', @now - INTERVAL 15 DAY);

-- Refunds. "automatic" = full refund with no owner review (pending cancelled, rejected, expired,
-- owner cancelled). "full"/"partial" = renter cancelled an approved booking; the owner reviews.
INSERT INTO Refunds (payment_id, policy_tier, cancellation_fee, refund_amount, refund_status,
                     requested_at, reviewed_at, processed_at, refund_receipt_url) VALUES
(5,  'automatic', 0,       13900.00, 'processed', @now - INTERVAL 3 DAY,  @now - INTERVAL 3 DAY,  @now - INTERVAL 3 DAY,  'receipts/refund-5.png'),
(6,  'automatic', 0,       14500.00, 'processed', @now - INTERVAL 3 DAY,  @now - INTERVAL 3 DAY,  @now - INTERVAL 3 DAY,  'receipts/refund-6.png'),
(16, 'full',      0,        8900.00, 'processed', @now - INTERVAL 15 DAY, @now - INTERVAL 14 DAY, @now - INTERVAL 14 DAY, 'receipts/refund-16.png'),
(17, 'partial',   3400.00, 13600.00, 'requested', @now - INTERVAL 1 DAY,  NULL, NULL, NULL),
(18, 'partial',   6120.00, 24480.00, 'processed', @now - INTERVAL 34 DAY, @now - INTERVAL 33 DAY, @now - INTERVAL 33 DAY, 'receipts/refund-18.png'),
(20, 'automatic', 0,       13900.00, 'processed', @now - INTERVAL 20 DAY, @now - INTERVAL 20 DAY, @now - INTERVAL 20 DAY, 'receipts/refund-20.png'),
(21, 'automatic', 0,       18500.00, 'processed', @now - INTERVAL 9 DAY,  @now - INTERVAL 9 DAY,  @now - INTERVAL 9 DAY,  'receipts/refund-21.png'),
(22, 'full',      0,        8900.00, 'requested', @now - INTERVAL 1 DAY,  NULL, NULL, NULL);

-- Rental income is recorded when the owner approves a booking (seeded as 5 hours after booking).
-- Every booking that was ever approved: approved, completed, and cancelled-after-approval.
INSERT INTO FinancialTransactions (property_id, payment_id, transaction_type, amount, transaction_date, note)
SELECT u.property_id, b.booking_id - 8800, 'rental_income', b.total_amount, b.created_at + INTERVAL 5 HOUR, NULL
FROM Bookings b JOIN Units u ON u.unit_id = b.unit_id
WHERE b.booking_status IN ('approved','completed')
   OR (b.booking_status = 'cancelled' AND b.booking_id <> 8821);

-- Refunds paid out on bookings that had already earned income.
INSERT INTO FinancialTransactions (property_id, payment_id, transaction_type, amount, transaction_date, note) VALUES
(2, 16, 'refund', 8900.00,  @now - INTERVAL 14 DAY, NULL),
(4, 18, 'refund', 24480.00, @now - INTERVAL 33 DAY, NULL),
(4, 20, 'refund', 13900.00, @now - INTERVAL 20 DAY, NULL),
(1, 30, 'damage_fee_income', 1200.00, @now - INTERVAL 15 DAY, NULL);

-- Monthly utilities for the last 6 months (this month included).
INSERT INTO FinancialTransactions (property_id, payment_id, transaction_type, amount, transaction_date, note)
SELECT p.property_id, NULL, 'utility_expense', p.amount,
       TIMESTAMP(DATE_FORMAT(@d - INTERVAL m.months MONTH, '%Y-%m-01'), '09:00'), 'Electricity and water'
FROM (SELECT 0 AS months UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5) m
CROSS JOIN (SELECT 1 AS property_id, 3200.00 AS amount UNION ALL SELECT 2, 7800.00 UNION ALL SELECT 3, 5400.00
            UNION ALL SELECT 4, 12500.00 UNION ALL SELECT 5, 2600.00 UNION ALL SELECT 6, 1900.00) p;

INSERT INTO FinancialTransactions (property_id, payment_id, transaction_type, amount, transaction_date, note) VALUES
(1, NULL, 'supply_expense',      2600.00, @now - INTERVAL 140 DAY, 'Bed sheets and towels'),
(1, NULL, 'maintenance_expense', 4500.00, @now - INTERVAL 60 DAY,  'Roof gutter repair'),
(1, NULL, 'supply_expense',       850.00, @now - INTERVAL 12 DAY,  'Hand soap and coffee'),
(2, NULL, 'maintenance_expense', 9800.00, @now - INTERVAL 100 DAY, 'Pool pump repair'),
(2, NULL, 'supply_expense',      3200.00, @now - INTERVAL 45 DAY,  'Toiletries for all suites'),
(2, NULL, 'maintenance_expense', 1500.00, @now - INTERVAL 5 DAY,   'Kettle replacement'),
(3, NULL, 'maintenance_expense', 6200.00, @now - INTERVAL 80 DAY,  'Water heater service'),
(3, NULL, 'supply_expense',      1900.00, @now - INTERVAL 30 DAY,  'Cleaning supplies'),
(4, NULL, 'maintenance_expense', 15000.00, @now - INTERVAL 25 DAY, 'Room 1 air conditioner repair'),
(4, NULL, 'supply_expense',      4800.00, @now - INTERVAL 70 DAY,  'Beach towels'),
(5, NULL, 'supply_expense',      1500.00, @now - INTERVAL 50 DAY,  'Firewood'),
(6, NULL, 'maintenance_expense', 2200.00, @now - INTERVAL 20 DAY,  'Terrace drain cleaning');

-- Messages (is_read = FALSE shows the unread dot)
INSERT INTO Messages (sender_id, receiver_id, booking_id, message_text, sent_at, is_read) VALUES
(3, 1, 8808, 'Hi, is there parking for two cars near The Loft?',             @now - INTERVAL 50 HOUR, TRUE),
(1, 3, 8808, 'Yes, there are two parking slots beside the gate.',             @now - INTERVAL 49 HOUR, TRUE),
(3, 1, 8808, 'Great. We will arrive around 3:00 PM.',                         @now - INTERVAL 30 HOUR, TRUE),
(1, 3, 8808, 'Noted. I will send the door code the morning of check-in.',     @now - INTERVAL 29 HOUR, FALSE),
(4, 1, 8807, 'Hello, is the suite close to the pool?',                        @now - INTERVAL 5 DAY,   TRUE),
(1, 4, 8807, 'Yes, Suite A opens to the courtyard and the pool.',             @now - INTERVAL 5 DAY + INTERVAL 1 HOUR, TRUE),
(5, 1, 8810, 'We have checked in. The cabin is lovely.',                      @now - INTERVAL 20 HOUR, TRUE),
(5, 1, 8810, 'Where can we find extra towels?',                               @now - INTERVAL 3 HOUR,  FALSE),
(7, 1, 8817, 'I had to cancel because my work schedule changed.',             @now - INTERVAL 23 HOUR, TRUE),
(1, 7, 8817, 'Understood. I will review your refund request.',                @now - INTERVAL 20 HOUR, TRUE),
(8, 1, 8812, 'We check out today. Thank you for the stay.',                   @now - INTERVAL 2 HOUR,  FALSE),
(1, 2, 8823, 'Hi Marco, is early check-in possible?',                         @now - INTERVAL 48 HOUR, TRUE),
(2, 1, 8823, 'Check-in is at 2:00 PM, but you can leave your bags from 11:00 AM.', @now - INTERVAL 46 HOUR, FALSE);

-- Inventory. unit_id NULL = shared items (and all items of a whole house).
INSERT INTO Items (property_id, unit_id, item_name, item_type, quantity, unit_price, item_condition, updated_at) VALUES
(1, NULL, 'Coffee maker',   'equipment',  1,  2500.00, 'good',        @now - INTERVAL 10 DAY),
(1, NULL, 'Bed sheets',     'consumable', 8,   650.00, 'good',        @now - INTERVAL 10 DAY),
(1, NULL, 'Bath towels',    'consumable', 12,  350.00, 'maintenance', @now - INTERVAL 2 DAY),
(1, NULL, 'TV',             'equipment',  1, 18000.00, 'good',        @now - INTERVAL 40 DAY),
(1, NULL, 'Rice cooker',    'equipment',  1,  1800.00, 'missing',     @now - INTERVAL 1 DAY),
(1, NULL, 'Hand soap',      'consumable', 6,    90.00, 'good',        @now - INTERVAL 12 DAY),
(1, NULL, 'Bedside lamp',   'equipment',  2,  1200.00, 'damaged',     @now - INTERVAL 16 DAY),
(2, NULL, 'Pool cleaning kit', 'equipment', 1, 3500.00, 'good',       @now - INTERVAL 30 DAY),
(2, NULL, 'Garden hose',    'equipment',  1,   900.00, 'good',        @now - INTERVAL 30 DAY),
(2, NULL, 'Trash bags',     'consumable', 30,   15.00, 'good',        @now - INTERVAL 7 DAY),
(2, 2,    'Bed sheets',     'consumable', 4,   650.00, 'good',        @now - INTERVAL 6 DAY),
(2, 2,    'Bath towels',    'consumable', 6,   350.00, 'good',        @now - INTERVAL 6 DAY),
(2, 2,    'Electric kettle','equipment',  1,  1500.00, 'damaged',     @now - INTERVAL 5 DAY),
(2, 2,    'TV',             'equipment',  1, 15000.00, 'good',        @now - INTERVAL 60 DAY),
(2, 3,    'Bed sheets',     'consumable', 6,   650.00, 'good',        @now - INTERVAL 9 DAY),
(2, 3,    'Bath towels',    'consumable', 8,   350.00, 'good',        @now - INTERVAL 9 DAY),
(2, 3,    'Hair dryer',     'equipment',  1,  1200.00, 'good',        @now - INTERVAL 50 DAY),
(2, 3,    'Shampoo bottles','consumable', 10,  120.00, 'good',        @now - INTERVAL 9 DAY),
(2, 4,    'Bed sheets',     'consumable', 8,   650.00, 'good',        @now - INTERVAL 4 DAY),
(2, 4,    'Bath towels',    'consumable', 10,  350.00, 'good',        @now - INTERVAL 4 DAY),
(2, 4,    'Air purifier',   'equipment',  1,  6500.00, 'maintenance', @now - INTERVAL 3 DAY),
(2, 4,    'Coffee maker',   'equipment',  1,  2500.00, 'good',        @now - INTERVAL 40 DAY),
(3, NULL, 'Bed sheets',     'consumable', 12,  650.00, 'good',        @now - INTERVAL 3 DAY),
(3, NULL, 'Bath towels',    'consumable', 18,  350.00, 'good',        @now - INTERVAL 3 DAY),
(3, NULL, 'Grill',          'equipment',  1,  8000.00, 'good',        @now - INTERVAL 70 DAY),
(3, NULL, 'Kayak',          'equipment',  2, 15000.00, 'good',        @now - INTERVAL 70 DAY),
(3, NULL, 'Dish soap',      'consumable', 5,    80.00, 'good',        @now - INTERVAL 3 DAY),
(4, NULL, 'Snorkel set',    'equipment',  6,   900.00, 'good',        @now - INTERVAL 20 DAY),
(4, NULL, 'Beach towels',   'consumable', 20,  400.00, 'good',        @now - INTERVAL 20 DAY),
(4, 6,    'Bed sheets',     'consumable', 4,   650.00, 'good',        @now - INTERVAL 8 DAY),
(4, 6,    'Mini fridge',    'equipment',  1,  9000.00, 'good',        @now - INTERVAL 90 DAY),
(4, 7,    'Bed sheets',     'consumable', 4,   650.00, 'good',        @now - INTERVAL 8 DAY),
(4, 7,    'Mini fridge',    'equipment',  1,  9000.00, 'good',        @now - INTERVAL 90 DAY),
(4, 8,    'Bed sheets',     'consumable', 6,   650.00, 'good',        @now - INTERVAL 8 DAY),
(4, 8,    'Air conditioner remote', 'equipment', 1, 600.00, 'missing', @now - INTERVAL 2 DAY),
(4, 9,    'Bed sheets',     'consumable', 6,   650.00, 'good',        @now - INTERVAL 8 DAY),
(4, 9,    'Safe box',       'equipment',  1,  4000.00, 'good',        @now - INTERVAL 90 DAY),
(5, NULL, 'Fireplace tools','equipment',  1,  2000.00, 'good',        @now - INTERVAL 45 DAY),
(5, NULL, 'Blankets',       'consumable', 6,   900.00, 'good',        @now - INTERVAL 45 DAY),
(5, NULL, 'Firewood bundles','consumable', 10, 150.00, 'good',        @now - INTERVAL 15 DAY),
(6, NULL, 'Bed sheets',     'consumable', 4,   650.00, 'good',        @now - INTERVAL 20 DAY),
(6, NULL, 'Bath towels',    'consumable', 6,   350.00, 'good',        @now - INTERVAL 20 DAY),
(6, NULL, 'Microwave',      'equipment',  1,  4500.00, 'good',        @now - INTERVAL 100 DAY);

-- Damage reports (owners only): one charged, one open, one dismissed.
INSERT INTO DamageReports (booking_id, description, photo_path, requested_fee, report_status, payment_id, created_at, resolved_at) VALUES
(8815, 'Bedside lamp shade broken.',                    NULL, 1200.00, 'charged',   30,   @now - INTERVAL 16 DAY, @now - INTERVAL 15 DAY),
(8813, 'Coffee stain on a sofa cushion.',               NULL, 2500.00, 'open',      NULL, @now - INTERVAL 35 DAY, NULL),
(8826, 'Scratch on the wooden floor near the entrance.', NULL, 4000.00, 'dismissed', NULL, @now - INTERVAL 90 DAY, @now - INTERVAL 88 DAY);
