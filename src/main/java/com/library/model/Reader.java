package com.library.model;

import java.time.LocalDate;

public record Reader(
        long id,
        String name,
        String cardNumber,
        LocalDate cardExpiry,
        double outstandingFine
) {
}
