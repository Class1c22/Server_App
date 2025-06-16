package dao;

import db_connect.DBManager;
import model.ProductTransaction;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProductTransactionDAO {

    private final ProductDAO productDAO;

    public ProductTransactionDAO(ProductDAO productDAO) {
        this.productDAO = productDAO;
    }

    // Додає транзакцію у межах переданого з’єднання
    private void addTransaction(Connection conn, ProductTransaction transaction) throws SQLException {
        String sql = "INSERT INTO inventory_operations (product_id, type, quantity, timestamp) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, transaction.getProductId());
            stmt.setString(2, transaction.getType());
            stmt.setInt(3, transaction.getQuantity());
            stmt.setTimestamp(4, Timestamp.valueOf(transaction.getTimestamp()));
            stmt.executeUpdate();
        }
    }

    // Обробка надходження товару (транзакція)
    public void processIncoming(int productId, int quantity) throws SQLException {
        try (Connection conn = DBManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                productDAO.addStock(conn, productId, quantity);

                ProductTransaction transaction = new ProductTransaction();
                transaction.setProductId(productId);
                transaction.setType("INCOMING");
                transaction.setQuantity(quantity);
                transaction.setTimestamp(LocalDateTime.now());

                addTransaction(conn, transaction);

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // Обробка списання товару (транзакція)
    public void processOutgoing(int productId, int quantity) throws SQLException {
        try (Connection conn = DBManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                productDAO.removeStock(conn, productId, quantity);

                ProductTransaction transaction = new ProductTransaction();
                transaction.setProductId(productId);
                transaction.setType("OUTGOING");
                transaction.setQuantity(quantity);
                transaction.setTimestamp(LocalDateTime.now());

                addTransaction(conn, transaction);

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // Отримати всі транзакції
    public List<ProductTransaction> getAllTransactions() throws SQLException {
        List<ProductTransaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM inventory_operations ORDER BY timestamp DESC";

        try (Connection conn = DBManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                transactions.add(mapResultSetToTransaction(rs));
            }
        }
        return transactions;
    }

    // Отримати транзакції за productId
    public List<ProductTransaction> getTransactionsByProductId(int productId) throws SQLException {
        List<ProductTransaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM inventory_operations WHERE product_id = ? ORDER BY timestamp DESC";

        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        }
        return transactions;
    }

    // Отримати транзакцію за id
    public ProductTransaction getTransactionById(int id) throws SQLException {
        String sql = "SELECT * FROM inventory_operations WHERE id = ?";

        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTransaction(rs);
                }
            }
        }
        return null;
    }

    // Отримати транзакції за типом (INCOMING або OUTGOING)
    public List<ProductTransaction> getTransactionsByType(String type) throws SQLException {
        List<ProductTransaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM inventory_operations WHERE type = ? ORDER BY timestamp DESC";

        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, type);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        }
        return transactions;
    }

    // Приватний метод мапінгу ResultSet в ProductTransaction
    private ProductTransaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        ProductTransaction transaction = new ProductTransaction();
        transaction.setId(rs.getInt("id"));
        transaction.setProductId(rs.getInt("product_id"));
        transaction.setType(rs.getString("type"));
        transaction.setQuantity(rs.getInt("quantity"));
        transaction.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
        return transaction;
    }
}
