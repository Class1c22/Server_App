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
    private Connection connection; // Use a real connection for tests

    @BeforeEach
    void setUp() throws SQLException {
        // Establish a real database connection for testing
        connection = DBManager.getConnection();
        connection.setAutoCommit(false); // We'll manage transactions manually for tests

        // Initialize DAOs with the real connection (or a factory that provides it)
        // Assuming ProductDAO also uses a Connection
        productDAO = new ProductDAO(connection); // Or ProductDAO(connection) if it needs it directly
        transactionDAO = new ProductTransactionDAO(productDAO); // Assuming it takes ProductDAO

        // Clean up any existing test data before each test
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM inventory_operations");
            stmt.executeUpdate("DELETE FROM products"); // Assuming products might be created/modified
        }
        // Insert a test product if needed for your tests to pass
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO products (id, name, stock) VALUES (?, ?, ?)")) {
            ps.setInt(1, 1);
            ps.setString(2, "Test Product 1");
            ps.setInt(3, 10); // Initial stock
            ps.executeUpdate();

            ps.setInt(1, 2);
            ps.setString(2, "Test Product 2");
            ps.setInt(3, 10); // Initial stock
            ps.executeUpdate();
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        // Rollback any changes made during the test to ensure test isolation
        if (connection != null) {
            connection.rollback();
            connection.close();
        }
    }

    @Test
    @DisplayName("processIncoming should add stock and insert transaction")
    void processIncoming_shouldAddStockAndInsertTransaction() throws Exception {
        int productId = 1;
        int initialStock = getProductStock(productId); // Get initial stock from DB
        int quantity = 5;

        transactionDAO.processIncoming(productId, quantity);

        // Verify stock has been increased in the actual database
        assertEquals(initialStock + quantity, getProductStock(productId),
                "Product stock should be increased after incoming transaction");

        // Verify a transaction record has been inserted
        assertTrue(hasTransactionRecord(productId, "INCOMING", quantity),
                "Incoming transaction record should be found in inventory_operations");
    }

    @Test
    @DisplayName("processOutgoing should remove stock and insert transaction")
    void processOutgoing_shouldRemoveStockAndInsertTransaction() throws Exception {
        int productId = 2;
        int initialStock = getProductStock(productId); // Get initial stock from DB
        int quantity = 3;

        transactionDAO.processOutgoing(productId, quantity);

        // Verify stock has been decreased in the actual database
        assertEquals(initialStock - quantity, getProductStock(productId),
                "Product stock should be decreased after outgoing transaction");

        // Verify a transaction record has been inserted
        assertTrue(hasTransactionRecord(productId, "OUTGOING", quantity),
                "Outgoing transaction record should be found in inventory_operations");
    }

    /**
     * Helper method to get the current stock of a product from the database.
     */
    private int getProductStock(int productId) throws SQLException {
        String sql = "SELECT stock FROM products WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("stock");
                }
            }
        }
        throw new SQLException("Product with ID " + productId + " not found.");
    }

    /**
     * Helper method to check if a transaction record exists in the database.
     */
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