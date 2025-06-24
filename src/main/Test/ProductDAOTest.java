import dao.ProductDAO;
import model.Product;
import db_connect.DBManager; // ✅ Правильний імпорт

import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductDAOTest {

    private static ProductDAO productDAO;

    @BeforeAll
    static void setup() throws SQLException {
        productDAO = new ProductDAO(DBManager.getConnection());

        try (Connection connection = DBManager.getConnection();
             Statement statement = connection.createStatement()) {


            statement.executeUpdate("DELETE FROM products");
            statement.executeUpdate("DELETE FROM product_groups");


            statement.executeUpdate("INSERT INTO product_groups (id, name, description) VALUES (1, 'TestGroup', 'Group for tests')");
        }
    }

    @Test
    @Order(1)
    void testAddProduct() throws SQLException {
        Product product = createTestProduct("Test1");

        productDAO.addProduct(product);

        List<Product> products = productDAO.getAllProducts();
        assertFalse(products.isEmpty(), "Product list should not be empty");

        Product fetched = products.stream().filter(p -> p.getName().equals("Test1")).findFirst().orElse(null);
        assertNotNull(fetched, "Product should be found");
        assertEquals("Test1", fetched.getName());
        assertEquals("Desc", fetched.getDescription());
    }

    @Test
    @Order(2)
    void testUpdateProduct() throws SQLException {
        Product product = createTestProduct("Test2");
        productDAO.addProduct(product);

        List<Product> products = productDAO.getAllProducts();
        Product toUpdate = products.stream().filter(p -> p.getName().equals("Test2")).findFirst().orElse(null);
        assertNotNull(toUpdate, "Product to update should exist");

        toUpdate.setDescription("Updated Desc");
        productDAO.updateProduct(toUpdate);

        Product updated = productDAO.getProductById(toUpdate.getId());
        assertNotNull(updated, "Updated product should be found");
        assertEquals("Updated Desc", updated.getDescription());
    }

    @Test
    @Order(3)
    void testAddStock() throws SQLException {
        Product product = createTestProduct("Test3");
        productDAO.addProduct(product);

        Product addedProduct = productDAO.getAllProducts().stream()
                .filter(p -> p.getName().equals("Test3"))
                .findFirst()
                .orElse(null);
        assertNotNull(addedProduct, "Product should exist");

        int originalQuantity = addedProduct.getQuantity();
        productDAO.addStock(addedProduct.getId(), 5);

        Product updated = productDAO.getProductById(addedProduct.getId());
        assertEquals(originalQuantity + 5, updated.getQuantity());
    }

    @Test
    @Order(4)
    void testRemoveStock() throws SQLException {
        Product product = createTestProduct("Test4");
        productDAO.addProduct(product);

        Product addedProduct = productDAO.getAllProducts().stream()
                .filter(p -> p.getName().equals("Test4"))
                .findFirst()
                .orElse(null);
        assertNotNull(addedProduct, "Product should exist");

        int originalQuantity = addedProduct.getQuantity();
        productDAO.removeStock(addedProduct.getId(), 3);

        Product updated = productDAO.getProductById(addedProduct.getId());
        assertEquals(originalQuantity - 3, updated.getQuantity());
    }

    @Test
    @Order(5)
    void testDeleteProduct() throws SQLException {
        Product product = createTestProduct("Test5");
        productDAO.addProduct(product);

        Product addedProduct = productDAO.getAllProducts().stream()
                .filter(p -> p.getName().equals("Test5"))
                .findFirst()
                .orElse(null);
        assertNotNull(addedProduct, "Product should exist");

        productDAO.deleteProduct(addedProduct.getId());

        Product deleted = productDAO.getProductById(addedProduct.getId());
        assertNull(deleted, "Product should be deleted");
    }

    @AfterAll
    static void teardown() throws SQLException {
        try (Connection connection = DBManager.getConnection();
             Statement statement = connection.createStatement()) {

            // Очистити таблиці після тестів
            statement.executeUpdate("DELETE FROM products");
            statement.executeUpdate("DELETE FROM product_groups");
        }
    }

    private Product createTestProduct(String name) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("Desc");
        product.setManufacturer("Maker");
        product.setQuantity(10);
        product.setPricePerUnit(new BigDecimal("100.00"));
        product.setGroupId(1);
        return product;
    }
}
