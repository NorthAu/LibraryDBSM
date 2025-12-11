package com.library.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class DatabaseManager {
    private static final String URL = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/library_db?useSSL=false&serverTimezone=UTC");
    private static final String USER = System.getenv().getOrDefault("DB_USER", "library_admin");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "library_admin");

    private static final DataSource dataSource = createDataSource();

    private DatabaseManager() {
    }

    private static DataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setUsername(USER);
        config.setPassword(PASSWORD);
        config.setMaximumPoolSize(10);
        config.setPoolName("library-db-pool");
        return new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
