package com.library.model;

public record Publisher(long id, String name) {
    @Override
    public String toString() {
        return name;
    }
}
