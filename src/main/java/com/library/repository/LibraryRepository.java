package com.library.repository;

import com.library.config.DatabaseManager;
import com.library.model.Book;
import com.library.model.BookDetail;
import com.library.model.Category;
import com.library.model.Loan;
import com.library.model.LoanDetail;
import com.library.model.Publisher;
import com.library.model.Reader;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LibraryRepository {
    private final DataSource dataSource;

    public LibraryRepository() {
        this.dataSource = DatabaseManager.getDataSource();
    }

    public void insertBook(Book book) throws SQLException {
        String sql = "INSERT INTO books (isbn, title, category_id, publisher_id, published_date, total_copies, available_copies) " +
                "VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE title=VALUES(title), category_id=VALUES(category_id), " +
                "publisher_id=VALUES(publisher_id), published_date=VALUES(published_date), total_copies=VALUES(total_copies), " +
                "available_copies=VALUES(available_copies)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, book.isbn());
            statement.setString(2, book.title());
            statement.setLong(3, book.categoryId());
            statement.setLong(4, book.publisherId());
            statement.setObject(5, book.publishedDate());
            statement.setInt(6, book.totalCopies());
            statement.setInt(7, book.availableCopies());
            statement.executeUpdate();
        }
    }

    public void insertReader(Reader reader) throws SQLException {
        String sql = "INSERT INTO readers (name, card_number, card_expiry, outstanding_fine) VALUES (?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE name=VALUES(name), card_expiry=VALUES(card_expiry)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, reader.name());
            statement.setString(2, reader.cardNumber());
            statement.setObject(3, reader.cardExpiry());
            statement.setDouble(4, reader.outstandingFine());
            statement.executeUpdate();
        }
    }

    public void borrowBook(long readerId, long bookId, LocalDate dueDate) throws SQLException {
        String sql = "INSERT INTO loans (book_id, reader_id, borrowed_date, due_date, renewals, fine_paid) VALUES (?,?,CURDATE(),?,0,0)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bookId);
            statement.setLong(2, readerId);
            statement.setObject(3, dueDate);
            statement.executeUpdate();
        }
    }

    public void renewLoan(long loanId, LocalDate newDueDate) throws SQLException {
        String sql = "UPDATE loans SET due_date=?, renewals=renewals+1 WHERE id=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, newDueDate);
            statement.setLong(2, loanId);
            statement.executeUpdate();
        }
    }

    public void returnBook(long loanId, double finePaid) throws SQLException {
        String sql = "UPDATE loans SET returned_date=CURDATE(), fine_paid=? WHERE id=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, finePaid);
            statement.setLong(2, loanId);
            statement.executeUpdate();
        }
    }

    public List<Loan> findLoansByReader(long readerId) throws SQLException {
        String sql = "SELECT id, book_id, reader_id, borrowed_date, due_date, returned_date, renewals, fine_paid FROM loans WHERE reader_id = ?";
        List<Loan> loans = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, readerId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                loans.add(new Loan(
                        rs.getLong("id"),
                        rs.getLong("book_id"),
                        rs.getLong("reader_id"),
                        rs.getObject("borrowed_date", LocalDate.class),
                        rs.getObject("due_date", LocalDate.class),
                        rs.getObject("returned_date", LocalDate.class),
                        rs.getInt("renewals"),
                        rs.getDouble("fine_paid")
                ));
            }
        }
        return loans;
    }

    public long upsertCategory(String name) throws SQLException {
        String sql = "INSERT INTO categories(name) VALUES (?) ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("无法获取分类 ID");
    }

    public long upsertPublisher(String name) throws SQLException {
        String sql = "INSERT INTO publishers(name) VALUES (?) ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("无法获取出版社 ID");
    }

    public List<Category> listCategories() throws SQLException {
        String sql = "SELECT id, name FROM categories ORDER BY name";
        List<Category> categories = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                categories.add(new Category(rs.getLong("id"), rs.getString("name")));
            }
        }
        return categories;
    }

    public List<Publisher> listPublishers() throws SQLException {
        String sql = "SELECT id, name FROM publishers ORDER BY name";
        List<Publisher> publishers = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                publishers.add(new Publisher(rs.getLong("id"), rs.getString("name")));
            }
        }
        return publishers;
    }

    public List<BookDetail> listBooks() throws SQLException {
        String sql = "SELECT b.id, b.isbn, b.title, b.category_id, c.name AS category_name, b.publisher_id, p.name AS publisher_name, " +
                "b.published_date, b.total_copies, b.available_copies " +
                "FROM books b JOIN categories c ON c.id = b.category_id JOIN publishers p ON p.id = b.publisher_id ORDER BY b.id DESC";
        List<BookDetail> books = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                books.add(new BookDetail(
                        rs.getLong("id"),
                        rs.getString("isbn"),
                        rs.getString("title"),
                        rs.getLong("category_id"),
                        rs.getString("category_name"),
                        rs.getLong("publisher_id"),
                        rs.getString("publisher_name"),
                        rs.getObject("published_date", LocalDate.class),
                        rs.getInt("total_copies"),
                        rs.getInt("available_copies")
                ));
            }
        }
        return books;
    }

    public List<Reader> listReaders() throws SQLException {
        String sql = "SELECT id, name, card_number, card_expiry, outstanding_fine FROM readers ORDER BY id DESC";
        List<Reader> readers = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                readers.add(new Reader(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("card_number"),
                        rs.getObject("card_expiry", LocalDate.class),
                        rs.getDouble("outstanding_fine")
                ));
            }
        }
        return readers;
    }

    public List<LoanDetail> listLoanDetails() throws SQLException {
        String sql = "SELECT l.id, l.book_id, b.title AS book_title, l.reader_id, r.name AS reader_name, l.borrowed_date, l.due_date, " +
                "l.returned_date, l.renewals, l.fine_paid FROM loans l " +
                "JOIN books b ON b.id = l.book_id JOIN readers r ON r.id = l.reader_id ORDER BY l.borrowed_date DESC, l.id DESC";
        List<LoanDetail> loans = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                loans.add(new LoanDetail(
                        rs.getLong("id"),
                        rs.getLong("book_id"),
                        rs.getString("book_title"),
                        rs.getLong("reader_id"),
                        rs.getString("reader_name"),
                        rs.getObject("borrowed_date", LocalDate.class),
                        rs.getObject("due_date", LocalDate.class),
                        rs.getObject("returned_date", LocalDate.class),
                        rs.getInt("renewals"),
                        rs.getDouble("fine_paid")
                ));
            }
        }
        return loans;
    }
}
