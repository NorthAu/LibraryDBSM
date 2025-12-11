package com.library.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String URL = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/library_db?useSSL=false&serverTimezone=UTC");
    private static final String USER = System.getenv().getOrDefault("DB_USER", "library_admin");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "library_admin");

    private static final DataSource dataSource = createDataSource();

    static {
        initializeSchema();
    }

    private DatabaseManager() {
    }

    private static DataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setUsername(USER);
        config.setPassword(PASSWORD);
        config.setMaximumPoolSize(10);
        config.setPoolName("library-db-pool");
        return new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }

    private static void initializeSchema() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS categories (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        name VARCHAR(100) NOT NULL UNIQUE
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS publishers (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        name VARCHAR(150) NOT NULL UNIQUE
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS books (
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
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS readers (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        name VARCHAR(150) NOT NULL,
                        card_number VARCHAR(50) NOT NULL UNIQUE,
                        card_expiry DATE NOT NULL,
                        outstanding_fine DECIMAL(10,2) NOT NULL DEFAULT 0
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS loans (
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
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS payments (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        reader_id BIGINT NOT NULL,
                        loan_id BIGINT NULL,
                        amount DECIMAL(10,2) NOT NULL,
                        paid_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_payments_reader FOREIGN KEY (reader_id) REFERENCES readers(id),
                        CONSTRAINT fk_payments_loan FOREIGN KEY (loan_id) REFERENCES loans(id)
                    )
                    """);

            statement.execute("DROP TRIGGER IF EXISTS trg_loans_insert");
            statement.execute("""
                    CREATE TRIGGER trg_loans_insert
                    AFTER INSERT ON loans
                    FOR EACH ROW
                    BEGIN
                        UPDATE books SET available_copies = available_copies - 1 WHERE id = NEW.book_id;
                    END
                    """);

            statement.execute("DROP TRIGGER IF EXISTS trg_loans_update");
            statement.execute("""
                    CREATE TRIGGER trg_loans_update
                    AFTER UPDATE ON loans
                    FOR EACH ROW
                    BEGIN
                        IF NEW.returned_date IS NOT NULL AND OLD.returned_date IS NULL THEN
                            UPDATE books SET available_copies = available_copies + 1 WHERE id = NEW.book_id;
                        END IF;
                    END
                    """);

            statement.execute("""
                    CREATE OR REPLACE VIEW view_book_stock AS
                    SELECT b.isbn, b.title, b.total_copies AS total_count, b.available_copies AS available_count
                    FROM books b
                    """);

            statement.execute("DROP PROCEDURE IF EXISTS get_reader_loans");
            statement.execute("""
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
                    END
                    """);

            statement.execute("INSERT IGNORE INTO categories(id, name) VALUES (1, '计算机'), (2, '文学')");
            statement.execute("INSERT IGNORE INTO publishers(id, name) VALUES (1, '机械工业出版社'), (2, '清华大学出版社')");
            statement.execute("""
                    INSERT IGNORE INTO books(id, isbn, title, category_id, publisher_id, published_date, total_copies, available_copies)
                    VALUES (1, '978-7-121-15535-2', 'Java 实战', 1, 1, '2020-01-01', 10, 10)
                    """);
            statement.execute("""
                    INSERT IGNORE INTO readers(id, name, card_number, card_expiry)
                    VALUES (1, '示例读者', 'CARD-001', DATE_ADD(CURDATE(), INTERVAL 1 YEAR))
                    """);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }
}
