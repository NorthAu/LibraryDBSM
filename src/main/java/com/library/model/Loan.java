package com.library.model;

import java.time.LocalDate;

public record Loan(
        long id,
        long bookId,
        long readerId,
        LocalDate borrowedDate,
        LocalDate dueDate,
        LocalDate returnedDate,
        int renewals,
        double finePaid
) {
}
