package com.library.model;

import java.time.LocalDate;

public record BookDetail(
        long id,
        String isbn,
        String title,
        long categoryId,
        String categoryName,
        long publisherId,
        String publisherName,
        LocalDate publishedDate,
        int totalCopies,
        int availableCopies
) {
    @Override
    public String toString() {
        return "%s (%s)".formatted(title, isbn);
    }
}
