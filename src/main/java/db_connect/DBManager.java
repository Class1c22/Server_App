package db_connect;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBManager {
    private static final String URL = "jdbc:mysql://localhost:3306/warehouse_db";
    private static final String USER = "root";
    private static final String PASSWORD = "пароль майскюель";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            if (conn != null) {
                System.out.println("З'єднання з базою даних встановлено!");
            }
        } catch (SQLException e) {
            System.err.println("Помилка з'єднання: " + e.getMessage());
        }
    }
}
