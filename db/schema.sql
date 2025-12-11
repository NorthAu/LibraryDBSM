-- 创建数据库和用户（可选）
CREATE DATABASE IF NOT EXISTS library_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'library_admin'@'%' IDENTIFIED BY 'library_admin';
GRANT ALL ON library_db.* TO 'library_admin'@'%';
USE library_db;

-- 基础表
CREATE TABLE categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE publishers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL UNIQUE
);

CREATE TABLE books (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    isbn VARCHAR(32) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    category_id BIGINT NOT NULL,
    publisher_id BIGINT NOT NULL,
    published_date DATE,
    total_copies INT NOT NULL,
    available_copies INT NOT NULL,
    CONSTRAINT fk_books_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_books_publisher FOREIGN KEY (publisher_id) REFERENCES publishers(id)
);

CREATE TABLE readers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    card_number VARCHAR(50) NOT NULL UNIQUE,
    card_expiry DATE NOT NULL,
    outstanding_fine DECIMAL(10,2) NOT NULL DEFAULT 0
);

CREATE TABLE loans (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    book_id BIGINT NOT NULL,
    reader_id BIGINT NOT NULL,
    borrowed_date DATE NOT NULL,
    due_date DATE NOT NULL,
    returned_date DATE NULL,
    renewals INT NOT NULL DEFAULT 0,
    fine_paid DECIMAL(10,2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_loans_book FOREIGN KEY (book_id) REFERENCES books(id),
    CONSTRAINT fk_loans_reader FOREIGN KEY (reader_id) REFERENCES readers(id)
);

CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reader_id BIGINT NOT NULL,
    loan_id BIGINT NULL,
    amount DECIMAL(10,2) NOT NULL,
    paid_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_reader FOREIGN KEY (reader_id) REFERENCES readers(id),
    CONSTRAINT fk_payments_loan FOREIGN KEY (loan_id) REFERENCES loans(id)
);

-- 触发器：借书时减少在册数量
DELIMITER $$
CREATE TRIGGER trg_loans_insert AFTER INSERT ON loans
FOR EACH ROW
BEGIN
    UPDATE books SET available_copies = available_copies - 1 WHERE id = NEW.book_id;
END$$
DELIMITER ;

-- 触发器：还书时增加在册数量
DELIMITER $$
CREATE TRIGGER trg_loans_update AFTER UPDATE ON loans
FOR EACH ROW
BEGIN
    IF NEW.returned_date IS NOT NULL AND OLD.returned_date IS NULL THEN
        UPDATE books SET available_copies = available_copies + 1 WHERE id = NEW.book_id;
    END IF;
END$$
DELIMITER ;

-- 视图：图书库存统计
CREATE OR REPLACE VIEW view_book_stock AS
SELECT b.isbn, b.title, b.total_copies AS total_count, b.available_copies AS available_count
FROM books b;

-- 存储过程：查询读者借阅情况
DELIMITER $$
CREATE PROCEDURE get_reader_loans(IN p_reader_id BIGINT)
BEGIN
    SELECT l.id AS loan_id,
           b.title AS book_title,
           l.borrowed_date,
           l.due_date,
           l.returned_date,
           l.renewals,
           l.fine_paid
    FROM loans l
    JOIN books b ON b.id = l.book_id
    WHERE l.reader_id = p_reader_id
    ORDER BY l.borrowed_date DESC;
END$$
DELIMITER ;

-- 示例数据
INSERT INTO categories(name) VALUES ('计算机'), ('文学');
INSERT INTO publishers(name) VALUES ('机械工业出版社'), ('清华大学出版社');
INSERT INTO books(isbn, title, category_id, publisher_id, published_date, total_copies, available_copies)
VALUES ('978-7-121-15535-2', 'Java 实战', 1, 1, '2020-01-01', 10, 10);
INSERT INTO readers(name, card_number, card_expiry) VALUES ('示例读者', 'CARD-001', DATE_ADD(CURDATE(), INTERVAL 1 YEAR));
