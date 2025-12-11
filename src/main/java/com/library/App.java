package com.library;

import com.library.model.Book;
import com.library.model.Reader;
import com.library.repository.LibraryRepository;
import com.library.service.LibraryService;

import java.time.LocalDate;

public class App {
    public static void main(String[] args) throws Exception {
        LibraryService service = new LibraryService(new LibraryRepository(), 1.5);

        // 示例：添加读者、添加图书、借阅、续借、归还
        service.addReader(new Reader(0, "示例读者", "CARD-001", LocalDate.now().plusYears(1), 0));
        service.addBook(new Book(0, "978-7-121-15535-2", "Java 实战", 1, 1, LocalDate.of(2020, 1, 1), 10, 10));

        // 借阅并续借
        service.borrowBook(1, 1, LocalDate.now().plusDays(30));
        service.renewLoan(1, LocalDate.now().plusDays(60));

        double fine = service.returnBook(1, LocalDate.now().plusDays(60), LocalDate.now().plusDays(65));
        System.out.printf("本次归还罚款金额：%.2f 元\n", fine);
    }
}
