package xyz.nexusservices.players.utils;

import xyz.nexusservices.players.Players;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class Database {
    private final HikariDataSource dataSource;

    public Database(Players players) {
        HikariConfig config = new HikariConfig();

        // Get database configuration from config.yml
        String host = players.getConfig().getString("MySQL.host");
        String username = players.getConfig().getString("MySQL.username");
        String password = players.getConfig().getString("MySQL.password");
        String database = players.getConfig().getString("MySQL.database");
        int port = players.getConfig().getInt("MySQL.port");

        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true";

        // Configure HikariCP
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(2000);

        this.dataSource = new HikariDataSource(config);
    }

    public void connect() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (connection == null) {
                throw new RuntimeException("Failed to connect to the database.");
            }
        }
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}