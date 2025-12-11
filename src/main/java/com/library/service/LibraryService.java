package com.library.service;

import com.library.model.Book;
import com.library.model.BookDetail;
import com.library.model.Category;
import com.library.model.Loan;
import com.library.model.LoanDetail;
import com.library.model.Publisher;
import com.library.model.Reader;
import com.library.repository.LibraryRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class LibraryService {
    private final LibraryRepository repository;
    private final double dailyFine;

    public LibraryService(LibraryRepository repository, double dailyFine) {
        this.repository = repository;
        this.dailyFine = dailyFine;
    }

    public void addBook(Book book) throws SQLException {
        repository.insertBook(book);
    }

    public void addReader(Reader reader) throws SQLException {
        repository.insertReader(reader);
    }

    public void borrowBook(long readerId, long bookId, LocalDate dueDate) throws SQLException {
        repository.borrowBook(readerId, bookId, dueDate);
    }

    public void renewLoan(long loanId, LocalDate newDueDate) throws SQLException {
        repository.renewLoan(loanId, newDueDate);
    }

    public double returnBook(long loanId, LocalDate dueDate, LocalDate returnedDate) throws SQLException {
        double fine = calculateFine(dueDate, returnedDate);
        repository.returnBook(loanId, fine);
        return fine;
    }

    public List<Loan> findLoansByReader(long readerId) throws SQLException {
        return repository.findLoansByReader(readerId);
    }

    public long saveCategory(String name) throws SQLException {
        return repository.upsertCategory(name);
    }

    public long savePublisher(String name) throws SQLException {
        return repository.upsertPublisher(name);
    }

    public List<Category> listCategories() throws SQLException {
        return repository.listCategories();
    }

    public List<Publisher> listPublishers() throws SQLException {
        return repository.listPublishers();
    }

    public List<BookDetail> listBooks() throws SQLException {
        return repository.listBooks();
    }

    public List<Reader> listReaders() throws SQLException {
        return repository.listReaders();
    }

    public List<LoanDetail> listLoanDetails() throws SQLException {
        return repository.listLoanDetails();
    }

    private double calculateFine(LocalDate dueDate, LocalDate returnedDate) {
        if (returnedDate.isAfter(dueDate)) {
            long overdueDays = ChronoUnit.DAYS.between(dueDate, returnedDate);
            return overdueDays * dailyFine;
        }
        return 0;
    }
}
