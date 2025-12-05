IF DB_ID('data_distributor') IS NULL
BEGIN
    CREATE DATABASE data_distributor;
END
GO

USE data_distributor;
GO

IF OBJECT_ID('dbo.signal_events', 'U') IS NOT NULL
    DROP TABLE dbo.signal_events;
GO
IF OBJECT_ID('dbo.signal_audit', 'U') IS NOT NULL
    DROP TABLE dbo.signal_audit;
GO
IF OBJECT_ID('dbo.signal', 'U') IS NOT NULL
    DROP TABLE dbo.signal;
GO
IF OBJECT_ID('dbo.ceh_response_initial_event_id', 'U') IS NOT NULL
    DROP TABLE dbo.ceh_response_initial_event_id;
GO
IF OBJECT_ID('dbo.account_balance_overview', 'U') IS NOT NULL
    DROP TABLE dbo.account_balance_overview;
GO
IF OBJECT_ID('dbo.product_configuration', 'U') IS NOT NULL
    DROP TABLE dbo.product_configuration;
GO
IF OBJECT_ID('dbo.product_risk_monitoring', 'U') IS NOT NULL
    DROP TABLE dbo.product_risk_monitoring;
GO

CREATE TABLE dbo.product_risk_monitoring (
    grv SMALLINT PRIMARY KEY,
    product_id SMALLINT NOT NULL,
    currency_code CHAR(3) NOT NULL,
    monitor_kraandicht CHAR(1) NOT NULL,
    monitor_CW014_signal CHAR(1) NOT NULL,
    report_CW014_to_ceh CHAR(1) NOT NULL,
    report_CW014_to_dial CHAR(1) NOT NULL
);
GO

INSERT INTO dbo.product_risk_monitoring (grv, product_id, currency_code, monitor_kraandicht, monitor_CW014_signal, report_CW014_to_ceh, report_CW014_to_dial)
VALUES
    (1, 101, 'EUR', 'Y', 'Y', 'Y', 'Y'),
    (2, 102, 'USD', 'Y', 'Y', 'Y', 'Y');
GO

CREATE TABLE dbo.product_configuration (
    grv SMALLINT PRIMARY KEY,
    product_id SMALLINT NOT NULL,
    monitor_kraandicht CHAR(1) NOT NULL,
    monitor_CW014_signal CHAR(1) NOT NULL,
    report_CW014_to_ceh CHAR(1) NOT NULL,
    report_CW014_to_dial CHAR(1) NOT NULL
);
GO

INSERT INTO dbo.product_configuration (grv, product_id, monitor_kraandicht, monitor_CW014_signal, report_CW014_to_ceh, report_CW014_to_dial)
VALUES
    (1, 101, 'Y', 'Y', 'Y', 'Y'),
    (2, 102, 'Y', 'Y', 'Y', 'Y');
GO

CREATE TABLE dbo.account_balance_overview (
    agreement_id BIGINT PRIMARY KEY,
    grv SMALLINT NOT NULL,
    iban CHAR(18) NOT NULL,
    life_cycle_status TINYINT NOT NULL,
    bc_number BIGINT NOT NULL,
    currency_code CHAR(3) NOT NULL,
    book_date DATE NOT NULL,
    unauthorized_debit_balance BIGINT NOT NULL,
    last_book_date_balance_cr_to_dt DATE NOT NULL,
    is_agreement_part_of_acbs CHAR(1) NOT NULL,
    is_margin_account_linked CHAR(1) NOT NULL DEFAULT 'N',
    CONSTRAINT FK_account_balance_overview_grv FOREIGN KEY (grv) REFERENCES dbo.product_risk_monitoring (grv)
);
GO

INSERT INTO dbo.account_balance_overview (agreement_id, grv, iban, life_cycle_status, bc_number, currency_code, book_date, unauthorized_debit_balance, last_book_date_balance_cr_to_dt, is_agreement_part_of_acbs)
VALUES
    (1001, 1, 'NL00TEST0123456789', 1, 20001, 'EUR', '2025-01-01', 500, '2025-01-01', 'Y'),
    (1002, 1, 'NL00TEST0123456790', 1, 20002, 'EUR', '2025-01-01', 600, '2025-01-01', 'Y'),
    (1003, 2, 'US00TEST0123456789', 1, 20003, 'USD', '2025-01-01', 700, '2025-01-01', 'Y'),
    (1004, 2, 'US00TEST0123456790', 1, 20004, 'USD', '2025-01-01', 800, '2025-01-01', 'Y'),
    (1005, 1, 'NL00TEST0123456791', 1, 20005, 'EUR', '2025-01-01', 900, '2025-01-01', 'Y');
GO

CREATE TABLE dbo.signal (
    signal_id BIGINT NOT NULL,
    agreement_id BIGINT NOT NULL,
    signal_start_date DATE NOT NULL,
    signal_end_date DATE NOT NULL,
    CONSTRAINT PK_signal PRIMARY KEY (signal_id)
);
GO

INSERT INTO dbo.signal (signal_id, agreement_id, signal_start_date, signal_end_date)
VALUES
    (1, 1001, '2025-01-01', '2025-01-04'),
    (2, 1002, '2025-01-01', '2025-01-04'),
    (3, 1003, '2025-01-01', '2025-01-04'),
    (4, 1004, '2025-01-01', '2025-01-04'),
    (5, 1005, '2025-01-01', '2025-01-04');
GO

CREATE TABLE dbo.signal_events (
    uabs_event_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    event_record_date_time DATETIME NOT NULL,
    event_type CHAR(20) NOT NULL,
    event_status CHAR(20) NOT NULL,
    unauthorized_debit_balance BIGINT NOT NULL,
    book_date DATE NOT NULL,
    grv SMALLINT NOT NULL,
    product_id SMALLINT NOT NULL,
    agreement_id BIGINT NOT NULL,
    signal_id BIGINT NOT NULL,
    CONSTRAINT FK_signal_events_signal_id FOREIGN KEY (signal_id) REFERENCES dbo.signal (signal_id)
);
GO

INSERT INTO dbo.signal_events (event_record_date_time, event_type, event_status, unauthorized_debit_balance, book_date, grv, product_id, agreement_id, signal_id)
VALUES
    ('2025-01-01 09:00', 'OVERLIMIT_SIGNAL', 'OVERLIMIT_SIGNAL', 500, '2025-01-01', 1, 101, 1001, 1),
    ('2025-01-02 09:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 450, '2025-01-02', 1, 101, 1001, 1),
    ('2025-01-03 09:00', 'PRODUCT_SWAP', 'PRODUCT_SWAP', 450, '2025-01-03', 1, 101, 1001, 1),
    ('2025-01-04 09:00', 'OUT_OF_OVERLIMIT', 'OUT_OF_OVERLIMIT', 0, '2025-01-04', 1, 101, 1001, 1),
    ('2025-01-01 10:00', 'OVERLIMIT_SIGNAL', 'OVERLIMIT_SIGNAL', 600, '2025-01-01', 1, 101, 1002, 2),
    ('2025-01-02 10:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 580, '2025-01-02', 1, 101, 1002, 2),
    ('2025-01-03 10:00', 'PRODUCT_SWAP', 'PRODUCT_SWAP', 580, '2025-01-03', 1, 101, 1002, 2),
    ('2025-01-04 10:00', 'OUT_OF_OVERLIMIT', 'OUT_OF_OVERLIMIT', 0, '2025-01-04', 1, 101, 1002, 2),
    ('2025-01-01 11:00', 'OVERLIMIT_SIGNAL', 'OVERLIMIT_SIGNAL', 700, '2025-01-01', 2, 102, 1003, 3),
    ('2025-01-02 11:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 650, '2025-01-02', 2, 102, 1003, 3),
    ('2025-01-03 11:00', 'PRODUCT_SWAP', 'PRODUCT_SWAP', 650, '2025-01-03', 2, 102, 1003, 3),
    ('2025-01-04 11:00', 'OUT_OF_OVERLIMIT', 'OUT_OF_OVERLIMIT', 0, '2025-01-04', 2, 102, 1003, 3),
    ('2025-01-01 12:00', 'OVERLIMIT_SIGNAL', 'OVERLIMIT_SIGNAL', 800, '2025-01-01', 2, 102, 1004, 4),
    ('2025-01-02 12:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 780, '2025-01-02', 2, 102, 1004, 4),
    ('2025-01-03 12:00', 'PRODUCT_SWAP', 'PRODUCT_SWAP', 780, '2025-01-03', 2, 102, 1004, 4),
    ('2025-01-04 12:00', 'OUT_OF_OVERLIMIT', 'OUT_OF_OVERLIMIT', 0, '2025-01-04', 2, 102, 1004, 4),
    ('2025-01-01 13:00', 'OVERLIMIT_SIGNAL', 'OVERLIMIT_SIGNAL', 900, '2025-01-01', 1, 101, 1005, 5),
    ('2025-01-02 13:00', 'FINANCIAL_UPDATE', 'FINANCIAL_UPDATE', 870, '2025-01-02', 1, 101, 1005, 5),
    ('2025-01-03 13:00', 'PRODUCT_SWAP', 'PRODUCT_SWAP', 870, '2025-01-03', 1, 101, 1005, 5),
    ('2025-01-04 13:00', 'OUT_OF_OVERLIMIT', 'OUT_OF_OVERLIMIT', 0, '2025-01-04', 1, 101, 1005, 5);
GO

CREATE TABLE dbo.signal_audit (
    audit_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    signal_id BIGINT NOT NULL,
    uabs_event_id BIGINT NOT NULL,
    consumer_id BIGINT NOT NULL,
    agreement_id BIGINT NOT NULL,
    unauthorized_debit_balance BIGINT NOT NULL,
    status CHAR(20) NOT NULL,
    response_code CHAR(10) NOT NULL,
    response_message CHAR(100) NOT NULL,
    audit_record_date_time DATETIME NOT NULL
);
GO

CREATE TABLE dbo.ceh_response_initial_event_id (
    ceh_initial_event_id VARCHAR(50) NOT NULL,
    signal_id BIGINT NOT NULL,
    PRIMARY KEY (ceh_initial_event_id, signal_id)
);
GO

CREATE INDEX IX_signal_events_agreement_id ON dbo.signal_events (agreement_id);
GO
