import db_connect.DBManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import dao.ProductTransactionDAO;
import dao.ProductDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class ProductTransactionDAOTest {

    private ProductDAO productDAO;
    private ProductTransactionDAO transactionDAO;
    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DBManager.getConnection();
        connection.setAutoCommit(false);

        productDAO = new ProductDAO(connection);
        transactionDAO = new ProductTransactionDAO(productDAO, connection);

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM inventory_operations");
            stmt.executeUpdate("DELETE FROM products");
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO products (id, name, quantity) VALUES (?, ?, ?)")) {
            ps.setInt(1, 1);
            ps.setString(2, "Test Product 1");
            ps.setInt(3, 10);
            ps.executeUpdate();

            ps.setInt(1, 2);
            ps.setString(2, "Test Product 2");
            ps.setInt(3, 10);
            ps.executeUpdate();
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null) {
            connection.rollback();
            connection.close();
        }
    }

    @Test
    @DisplayName("processIncoming should add stock and insert transaction")
    void processIncoming_shouldAddStockAndInsertTransaction() throws Exception {
        int productId = 1;
        int initialStock = getProductStock(productId);
        int quantity = 5;

        transactionDAO.processIncoming(productId, quantity);

        assertEquals(initialStock + quantity, getProductStock(productId),
                "Product stock should be increased after incoming transaction");

        assertTrue(hasTransactionRecord(productId, "IN", quantity),
                "Incoming transaction record should be found in inventory_operations");
    }

    @Test
    @DisplayName("processOutgoing should remove stock and insert transaction")
    void processOutgoing_shouldRemoveStockAndInsertTransaction() throws Exception {
        int productId = 2;
        int initialStock = getProductStock(productId);
        int quantity = 3;

        transactionDAO.processOutgoing(productId, quantity);

        assertEquals(initialStock - quantity, getProductStock(productId),
                "Product stock should be decreased after outgoing transaction");

        assertTrue(hasTransactionRecord(productId, "OUT", quantity),
                "Outgoing transaction record should be found in inventory_operations");
    }

    private int getProductStock(int productId) throws SQLException {
        String sql = "SELECT quantity FROM products WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("quantity");
                }
            }
        }
        throw new SQLException("Product with ID " + productId + " not found.");
    }

    private boolean hasTransactionRecord(int productId, String operationType, int quantity) throws SQLException {
        String sql = "SELECT COUNT(*) FROM inventory_operations WHERE product_id = ? AND operation_type = ? AND quantity = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setString(2, operationType);
            ps.setInt(3, quantity);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
}
