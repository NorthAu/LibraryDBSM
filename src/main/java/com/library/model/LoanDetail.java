package com.library.model;

import java.time.LocalDate;

public record LoanDetail(
        long id,
        long bookId,
        String bookTitle,
        long readerId,
        String readerName,
        LocalDate borrowedDate,
        LocalDate dueDate,
        LocalDate returnedDate,
        int renewals,
        double finePaid
) {
}
