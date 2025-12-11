package com.library.repository;

import com.library.config.DatabaseManager;
import com.library.model.Book;
import com.library.model.Loan;
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
        String sql = "INSERT INTO books (isbn, title, category_id, publisher_id, published_date, total_copies, available_copies) VALUES (?,?,?,?,?,?,?)";
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
        String sql = "INSERT INTO readers (name, card_number, card_expiry, outstanding_fine) VALUES (?,?,?,?)";
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
}
