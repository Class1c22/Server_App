package dao;

import db_connect.DBManager;
import model.ProductTransaction;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

public class ProductTransactionDAO {

    public void addTransaction(ProductTransaction transaction) throws SQLException {
        String sql = "INSERT INTO inventory_operations (product_id, type, quantity, timestamp) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, transaction.getProductId());
            stmt.setString(2, transaction.getType());
            stmt.setInt(3, transaction.getQuantity());
            stmt.setTimestamp(4, Timestamp.valueOf(transaction.getTimestamp()));

            stmt.executeUpdate();
        }
    }

    // Process incoming stock with transaction record
    public void processIncoming(int productId, int quantity, ProductDAO productDAO) throws SQLException {
        Connection conn = null;
        try {
            conn = DBManager.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // Add stock to product
            productDAO.addStock(productId, quantity);

            // Record transaction
            ProductTransaction transaction = new ProductTransaction();
            transaction.setProductId(productId);
            transaction.setType("INCOMING");
            transaction.setQuantity(quantity);
            transaction.setTimestamp(LocalDateTime.now());

            addTransaction(transaction);

            conn.commit(); // Commit transaction
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback(); // Rollback on error
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    // Process outgoing stock with transaction record
    public void processOutgoing(int productId, int quantity, ProductDAO productDAO) throws SQLException {
        Connection conn = null;
        try {
            conn = DBManager.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // Remove stock from product
            productDAO.removeStock(productId, quantity);

            // Record transaction
            ProductTransaction transaction = new ProductTransaction();
            transaction.setProductId(productId);
            transaction.setType("OUTGOING");
            transaction.setQuantity(quantity);
            transaction.setTimestamp(LocalDateTime.now());

            addTransaction(transaction);

            conn.commit(); // Commit transaction
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback(); // Rollback on error
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

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