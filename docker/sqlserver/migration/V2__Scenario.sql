-- ##################################################################
-- # SIGNAL INSERTS (to satisfy FK constraint for Signal IDs 6-16)
-- ##################################################################

-- Scenario 1 (Signal ID 6 - Closed)
INSERT INTO dbo.signal (signal_id, agreement_id, signal_start_date, signal_end_date)
VALUES (6, 1001, '2025-01-06', '2025-01-10');

-- Scenario 2 (Signal ID 7 - Closed)
INSERT INTO dbo.signal (signal_id, agreement_id, signal_start_date, signal_end_date)
VALUES (7, 1002, '2025-02-01', '2025-02-05');

-- Scenario 3 (Signal ID 8 - Open)
INSERT INTO dbo.signal (signal_id, agreement_id, signal_start_date, signal_end_date)
VALUES (8, 1003, '2025-03-01', '9999-12-31');

-- Scenario 4 (Signal ID 9 - Open)
INSERT INTO dbo.signal (signal_id, agreement_id, signal_start_date, signal_end_date)
VALUES (9, 1004, '2025-04-01', '9999-12-31');

-- Scenario 5 (Signal ID 10 - Open)
INSERT INTO dbo.signal (signal_id, agreement_id, signal_start_date, signal_end_date)
VALUES (10, 1005, '2025-05-01', '9999-12-31');

-- Scenario 6 (Signal ID 11 - Open)
INSERT INTO dbo.signal (signal_id, agreement_id, signal_start_date, signal_end_date)
VALUES (11, 1001, '2025-06-01', '9999-12-31');

-- Scenario 7 (Signal ID 12 - Open)
INSERT INTO dbo.signal (signal_id, agreement_id, signal_start_date, signal_end_date)
VALUES (12, 1002, '2025-07-01', '9999-12-31');

-- Scenario 8 (Signal ID 13 - Open)
INSERT INTO dbo.signal (signal_id, agreement_id, signal_start_date, signal_end_date)
VALUES (13, 1003, '2025-08-01', '9999-12-31');

-- Scenario 9 (Signal ID 14 - Open Below Threshold, Overdue Test)
INSERT INTO dbo.signal (signal_id, agreement_id, signal_start_date, signal_end_date)
VALUES (14, 1001, '2025-09-01', '9999-12-31');

-- Scenario 10 (Signal ID 15 - Breach on DPD 0)
INSERT INTO dbo.signal (signal_id, agreement_id, signal_start_date, signal_end_date)
VALUES (15, 1001, '2025-09-10', '9999-12-31');

-- Scenario 11 (Signal ID 16 - Quick Closure Below Threshold)
INSERT INTO dbo.signal (signal_id, agreement_id, signal_start_date, signal_end_date)
VALUES (16, 1001, '2025-09-15', '2025-09-17');

-- Scenario 12 (Signal ID 17 - Closure After Initial Send)
INSERT INTO dbo.signal (signal_id, agreement_id, signal_start_date, signal_end_date)
VALUES (17, 1002, '2025-10-01', '2025-10-05');

-- Scenario 13 (Signal ID 18 - Product Swap Follow-up)
INSERT INTO dbo.signal (signal_id, agreement_id, signal_start_date, signal_end_date)
VALUES (18, 1003, '2025-10-10', '9999-12-31');

-- Scenario 14 (Signal ID 19 - Failed Initial, Subsequent Breach)
INSERT INTO dbo.signal (signal_id, agreement_id, signal_start_date, signal_end_date)
VALUES (19, 1004, '2025-10-20', '9999-12-31');

-- Scenario 15 (Signal ID 20 - PRODUCT_SWAP at DPD 0)
INSERT INTO dbo.signal (signal_id, agreement_id, signal_start_date, signal_end_date)
VALUES (20, 1005, '2025-11-01', '9999-12-31');

-- Scenario 16 (Signal ID 21 - Open, FU, SWAP, FU - Long Open)
INSERT INTO dbo.signal (signal_id, agreement_id, signal_start_date, signal_end_date)
VALUES (21, 1001, '2025-11-10', '9999-12-31');
GO


-- ##################################################################
-- # SIGNAL_EVENTS INSERTS
-- ##################################################################

INSERT INTO dbo.signal_events (event_record_date_time, event_type, event_status, unauthorized_debit_balance, book_date, grv, product_id, agreement_id, signal_id)
VALUES
-- =========================================================================================================
-- SCENARIO 1: Open 10 -> FU 20 -> FU 250 -> FU 249 -> CLOSE (Signal ID 6)
-- GRV 1, Product 101, Agreement 1001
    ('2025-01-06 10:00', 'OVERLIMIT_SIGNAL', 'OVERLIMIT_SIGNAL', 10, '2025-01-06', 1, 101, 1001, 6), -- DPD 0
    ('2025-01-07 11:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 20, '2025-01-07', 1, 101, 1001, 6), -- DPD 1
    ('2025-01-08 12:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 250, '2025-01-08', 1, 101, 1001, 6), -- DPD 2
    ('2025-01-09 13:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 249, '2025-01-09', 1, 101, 1001, 6), -- DPD 3
    ('2025-01-10 14:00', 'OUT_OF_OVERLIMIT', 'OUT_OF_OVERLIMIT', 0, '2025-01-10', 1, 101, 1001, 6), -- DPD 4

-- =========================================================================================================
-- SCENARIO 2: Open 10 -> FU 20 -> CLOSE (DPD 4) (Signal ID 7)
-- GRV 1, Product 101, Agreement 1002
    ('2025-02-01 10:00', 'OVERLIMIT_SIGNAL', 'OVERLIMIT_SIGNAL', 10, '2025-02-01', 1, 101, 1002, 7), -- DPD 0
    ('2025-02-02 11:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 20, '2025-02-02', 1, 101, 1002, 7), -- DPD 1
    ('2025-02-05 14:00', 'OUT_OF_OVERLIMIT', 'OUT_OF_OVERLIMIT', 0, '2025-02-05', 1, 101, 1002, 7), -- DPD 4

-- =========================================================================================================
-- SCENARIO 3: Open 10 -> FU 20 -> FU 249 (Open) (Signal ID 8)
-- GRV 2, Product 102, Agreement 1003
    ('2025-03-01 10:00', 'OVERLIMIT_SIGNAL', 'OVERLIMIT_SIGNAL', 10, '2025-03-01', 2, 102, 1003, 8), -- DPD 0
    ('2025-03-02 11:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 20, '2025-03-02', 2, 102, 1003, 8), -- DPD 1
    ('2025-03-03 12:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 249, '2025-03-03', 2, 102, 1003, 8), -- DPD 2

-- =========================================================================================================
-- SCENARIO 4: Open 10 -> FU 20 -> FU 249 -> FU 250 (DPD 6) (Signal ID 9)
-- GRV 2, Product 102, Agreement 1004
    ('2025-04-01 10:00', 'OVERLIMIT_SIGNAL', 'OVERLIMIT_SIGNAL', 10, '2025-04-01', 2, 102, 1004, 9), -- DPD 0
    ('2025-04-02 11:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 20, '2025-04-02', 2, 102, 1004, 9), -- DPD 1
    ('2025-04-03 12:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 249, '2025-04-03', 2, 102, 1004, 9), -- DPD 2
    ('2025-04-07 13:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 250, '2025-04-07', 2, 102, 1004, 9), -- DPD 6

-- =========================================================================================================
-- SCENARIO 5: Open 10 -> FU 20 -> FU 249 -> FU 250 (DPD 7) (Signal ID 10)
-- GRV 1, Product 101, Agreement 1005
    ('2025-05-01 10:00', 'OVERLIMIT_SIGNAL', 'OVERLIMIT_SIGNAL', 10, '2025-05-01', 1, 101, 1005, 10), -- DPD 0
    ('2025-05-02 11:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 20, '2025-05-02', 1, 101, 1005, 10), -- DPD 1
    ('2025-05-03 12:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 249, '2025-05-03', 1, 101, 1005, 10), -- DPD 2
    ('2025-05-08 13:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 250, '2025-05-08', 1, 101, 1005, 10), -- DPD 7

-- =========================================================================================================
-- SCENARIO 6: Open 10 (Only) (Signal ID 11)
-- GRV 1, Product 101, Agreement 1001
    ('2025-06-01 10:00', 'OVERLIMIT_SIGNAL', 'OVERLIMIT_SIGNAL', 10, '2025-06-01', 1, 101, 1001, 11), -- DPD 0

-- =========================================================================================================
-- SCENARIO 7: Open 10 -> FU 250 -> FU 251 (DPD 7) (Signal ID 12)
-- GRV 1, Product 101, Agreement 1002
    ('2025-07-01 10:00', 'OVERLIMIT_SIGNAL', 'OVERLIMIT_SIGNAL', 10, '2025-07-01', 1, 101, 1002, 12), -- DPD 0
    ('2025-07-02 11:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 250, '2025-07-02', 1, 101, 1002, 12), -- DPD 1
    ('2025-07-08 12:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 251, '2025-07-08', 1, 101, 1002, 12), -- DPD 7

-- =========================================================================================================
-- SCENARIO 8: Open 10 -> FU 250 -> FU 10 -> FU 20 (Signal ID 13)
-- GRV 2, Product 102, Agreement 1003
    ('2025-08-01 10:00', 'OVERLIMIT_SIGNAL', 'OVERLIMIT_SIGNAL', 10, '2025-08-01', 2, 102, 1003, 13), -- DPD 0
    ('2025-08-02 11:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 250, '2025-08-02', 2, 102, 1003, 13), -- DPD 1
    ('2025-08-03 12:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 10, '2025-08-03', 2, 102, 1003, 13), -- DPD 2 (Drop)
    ('2025-08-04 13:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 20, '2025-08-04', 2, 102, 1003, 13), -- DPD 3 (Re-increase)

-- =========================================================================================================
-- SCENARIO 9: Open 10 (Open Below Threshold, Overdue Test for DPD 7) (Signal ID 14)
-- GRV 1, Product 101, Agreement 1001
    ('2025-09-01 10:00', 'OVERLIMIT_SIGNAL', 'OVERLIMIT_SIGNAL', 10, '2025-09-01', 1, 101, 1001, 14), -- DPD 0

-- =========================================================================================================
-- SCENARIO 10: Open 10 -> FU 150 (Same Day Breach) (Signal ID 15)
-- GRV 1, Product 101, Agreement 1001
    ('2025-09-10 09:00', 'OVERLIMIT_SIGNAL', 'OVERLIMIT_SIGNAL', 10, '2025-09-10', 1, 101, 1001, 15), -- DPD 0 (Earliest, non-breach)
    ('2025-09-10 11:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 150, '2025-09-10', 1, 101, 1001, 15), -- DPD 0 (Later, breach)

-- =========================================================================================================
-- SCENARIO 11: Open 10 -> Close 0 (DPD 2 - Quick Closure Skip Test) (Signal ID 16)
-- GRV 1, Product 101, Agreement 1001
    ('2025-09-15 10:00', 'OVERLIMIT_SIGNAL', 'OVERLIMIT_SIGNAL', 10, '2025-09-15', 1, 101, 1001, 16), -- DPD 0
    ('2025-09-17 11:00', 'OUT_OF_OVERLIMIT', 'OUT_OF_OVERLIMIT', 0, '2025-09-17', 1, 101, 1001, 16), -- DPD 2

-- =========================================================================================================
-- SCENARIO 12: Open 150 -> Close 0 (DPD 4 - Closure Follow-up Test) (Signal ID 17)
-- GRV 1, Product 101, Agreement 1002
    ('2025-10-01 10:00', 'OVERLIMIT_SIGNAL', 'OVERLIMIT_SIGNAL', 150, '2025-10-01', 1, 101, 1002, 17), -- DPD 0 (Breach)
    ('2025-10-05 11:00', 'OUT_OF_OVERLIMIT', 'OUT_OF_OVERLIMIT', 0, '2025-10-05', 1, 101, 1002, 17), -- DPD 4 (Closure)

-- =========================================================================================================
-- SCENARIO 13: Open 150 -> PRODUCT_SWAP 150 (Follow-up Test) (Signal ID 18)
-- GRV 2, Product 102, Agreement 1003
    ('2025-10-10 10:00', 'OVERLIMIT_SIGNAL', 'OVERLIMIT_SIGNAL', 150, '2025-10-10', 2, 102, 1003, 18), -- DPD 0 (Breach)
    ('2025-10-11 11:00', 'PRODUCT_SWAP', 'PRODUCT_SWAP', 150, '2025-10-11', 2, 102, 1003, 18), -- DPD 1 (Non-financial change)

-- =========================================================================================================
-- SCENARIO 14: Open 150 -> FU 200 (Failed Initial Send Test) (Signal ID 19)
-- GRV 2, Product 102, Agreement 1004
    ('2025-10-20 10:00', 'OVERLIMIT_SIGNAL', 'OVERLIMIT_SIGNAL', 150, '2025-10-20', 2, 102, 1004, 19), -- DPD 0 (Breach)
    ('2025-10-21 11:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 200, '2025-10-21', 2, 102, 1004, 19), -- DPD 1 (Breach)

-- =========================================================================================================
-- SCENARIO 15: PRODUCT_SWAP at DPD 0 (Signal ID 20)
-- GRV 1, Product 101, Agreement 1005
    ('2025-11-01 10:00', 'OVERLIMIT_SIGNAL', 'OVERLIMIT_SIGNAL', 10, '2025-11-01', 1, 101, 1005, 20), -- DPD 0 (Open)
    ('2025-11-01 11:00', 'PRODUCT_SWAP', 'PRODUCT_SWAP', 10, '2025-11-01', 1, 101, 1005, 20), -- DPD 0 (Swap)
    ('2025-11-02 12:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 100, '2025-11-02', 1, 101, 1005, 20), -- DPD 1 (Breach)

-- =========================================================================================================
-- SCENARIO 16: Open -> FU -> SWAP -> FU (Long Open) (Signal ID 21)
-- GRV 1, Product 101, Agreement 1001
    ('2025-11-10 10:00', 'OVERLIMIT_SIGNAL', 'OVERLIMIT_SIGNAL', 150, '2025-11-10', 1, 101, 1001, 21), -- DPD 0
    ('2025-11-11 11:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 160, '2025-11-11', 1, 101, 1001, 21), -- DPD 1
    ('2025-11-12 12:00', 'PRODUCT_SWAP', 'PRODUCT_SWAP', 160, '2025-11-12', 1, 101, 1001, 21), -- DPD 2
    ('2025-11-18 13:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 170, '2025-11-18', 1, 101, 1001, 21); -- DPD 8
GO