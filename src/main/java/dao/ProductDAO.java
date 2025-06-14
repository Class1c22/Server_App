package dao;

import model.Product;
import db_connect.DBManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

public class ProductDAO {

    public void addProduct(Product product) throws SQLException {
        // Check if product name already exists
        if (isProductNameExists(product.getName())) {
            throw new SQLException("Product name '" + product.getName() + "' already exists. Product names must be unique.");
        }

        String sql = "INSERT INTO products (name, description, manufacturer, quantity, price_per_unit, group_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, product.getName());
            stmt.setString(2, product.getDescription());
            stmt.setString(3, product.getManufacturer());
            stmt.setInt(4, product.getQuantity());
            stmt.setBigDecimal(5, product.getPricePerUnit());
            stmt.setInt(6, product.getGroupId());

            stmt.executeUpdate();
        }
    }

    public void updateProduct(Product product) throws SQLException {
        // Check if product name exists for other products
        if (isProductNameExistsForOthers(product.getName(), product.getId())) {
            throw new SQLException("Product name '" + product.getName() + "' already exists. Product names must be unique.");
        }

        String sql = "UPDATE products SET name = ?, description = ?, manufacturer = ?, quantity = ?, price_per_unit = ?, group_id = ? WHERE id = ?";

        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, product.getName());
            stmt.setString(2, product.getDescription());
            stmt.setString(3, product.getManufacturer());
            stmt.setInt(4, product.getQuantity());
            stmt.setBigDecimal(5, product.getPricePerUnit());
            stmt.setInt(6, product.getGroupId());
            stmt.setInt(7, product.getId());

            stmt.executeUpdate();
        }
    }

    public void deleteProduct(int id) throws SQLException {
        String sql = "DELETE FROM products WHERE id = ?";

        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // New method: Add stock (incoming inventory)
    public void addStock(int productId, int quantity) throws SQLException {
        String updateSql = "UPDATE products SET quantity = quantity + ? WHERE id = ?";

        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {

            stmt.setInt(1, quantity);
            stmt.setInt(2, productId);
            stmt.executeUpdate();
        }
    }

    // New method: Remove stock (outgoing inventory/sales)
    public void removeStock(int productId, int quantity) throws SQLException {
        // First check if enough stock available
        Product product = getProductById(productId);
        if (product == null) {
            throw new SQLException("Product not found");
        }

        if (product.getQuantity() < quantity) {
            throw new SQLException("Insufficient stock. Available: " + product.getQuantity() + ", Requested: " + quantity);
        }

        String updateSql = "UPDATE products SET quantity = quantity - ? WHERE id = ?";

        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {

            stmt.setInt(1, quantity);
            stmt.setInt(2, productId);
            stmt.executeUpdate();
        }
    }

    public Product getProductById(int id) throws SQLException {
        String sql = "SELECT * FROM products WHERE id = ?";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToProduct(rs);
            }
            return null;
        }
    }

    public List<Product> getAllProducts() throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products ORDER BY name";

        try (Connection conn = DBManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        }
        return products;
    }

    public List<Product> getProductsByGroupId(int groupId) throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE group_id = ? ORDER BY name";

        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, groupId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        }
        return products;
    }

    public List<Product> searchProducts(String keyword) throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE name LIKE ? OR description LIKE ? OR manufacturer LIKE ? ORDER BY name";

        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String likeKeyword = "%" + keyword + "%";
            stmt.setString(1, likeKeyword);
            stmt.setString(2, likeKeyword);
            stmt.setString(3, likeKeyword);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        }

        return products;
    }

    // Statistics methods
    public BigDecimal getTotalInventoryValue() throws SQLException {
        String sql = "SELECT SUM(quantity * price_per_unit) as total_value FROM products";

        try (Connection conn = DBManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getBigDecimal("total_value");
            }
            return BigDecimal.ZERO;
        }
    }

    public BigDecimal getGroupTotalValue(int groupId) throws SQLException {
        String sql = "SELECT SUM(quantity * price_per_unit) as total_value FROM products WHERE group_id = ?";

        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, groupId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("total_value");
            }
            return BigDecimal.ZERO;
        }
    }

    private boolean isProductNameExists(String name) throws SQLException {
        String sql = "SELECT COUNT(*) FROM products WHERE name = ?";

        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        }
    }

    private boolean isProductNameExistsForOthers(String name, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM products WHERE name = ? AND id != ?";

        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            stmt.setInt(2, excludeId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        }
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        Product product = new Product();

        product.setId(rs.getInt("id"));
        product.setName(rs.getString("name"));
        product.setDescription(rs.getString("description"));
        product.setManufacturer(rs.getString("manufacturer"));
        product.setQuantity(rs.getInt("quantity"));
        product.setPricePerUnit(rs.getBigDecimal("price_per_unit"));
        product.setGroupId(rs.getInt("group_id"));

        return product;
    }
}