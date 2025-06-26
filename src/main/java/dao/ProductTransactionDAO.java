package dao;

import model.ProductTransaction;

import java.sql.*;
import java.time.LocalDateTime;


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

}
