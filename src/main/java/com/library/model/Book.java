package com.library.model;

import java.time.LocalDate;

public record Book(
        long id,
        String isbn,
        String title,
        long categoryId,
        long publisherId,
        LocalDate publishedDate,
        int totalCopies,
        int availableCopies
) {
}
