package dao;

import model.ProductTransaction;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProductTransactionDAO {

    private final ProductDAO productDAO;
    private final Connection connection;

    public ProductTransactionDAO(ProductDAO productDAO, Connection connection) {
        this.productDAO = productDAO;
        this.connection = connection;
    }

    private void addTransaction(ProductTransaction transaction) throws SQLException {
        String sql = "INSERT INTO inventory_operations (product_id, operation_type, quantity, operation_date) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, transaction.getProductId());
            stmt.setString(2, transaction.getType());
            stmt.setInt(3, transaction.getQuantity());
            stmt.setTimestamp(4, Timestamp.valueOf(transaction.getTimestamp()));
            stmt.executeUpdate();
        }
    }

    public void processIncoming(int productId, int quantity) throws SQLException {
        connection.setAutoCommit(false);
        try {
            productDAO.addStock(connection, productId, quantity);

            ProductTransaction transaction = new ProductTransaction();
            transaction.setProductId(productId);
            transaction.setType("IN");
            transaction.setQuantity(quantity);
            transaction.setTimestamp(LocalDateTime.now());

            addTransaction(transaction);

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }

    public void processOutgoing(int productId, int quantity) throws SQLException {
        connection.setAutoCommit(false);
        try {
            productDAO.removeStock(connection, productId, quantity);

            ProductTransaction transaction = new ProductTransaction();
            transaction.setProductId(productId);
            transaction.setType("OUT");
            transaction.setQuantity(quantity);
            transaction.setTimestamp(LocalDateTime.now());

            addTransaction(transaction);

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }

    public List<ProductTransaction> getAllTransactions() throws SQLException {
        List<ProductTransaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM inventory_operations ORDER BY operation_date DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                transactions.add(mapResultSetToTransaction(rs));
            }
        }
        return transactions;
    }

    public List<ProductTransaction> getTransactionsByProductId(int productId) throws SQLException {
        List<ProductTransaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM inventory_operations WHERE product_id = ? ORDER BY operation_date DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
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

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
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
        String sql = "SELECT * FROM inventory_operations WHERE operation_type = ? ORDER BY operation_date DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
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
        transaction.setType(rs.getString("operation_type"));
        transaction.setQuantity(rs.getInt("quantity"));
        transaction.setTimestamp(rs.getTimestamp("operation_date").toLocalDateTime()); // було timestamp
        return transaction;
    }
}
