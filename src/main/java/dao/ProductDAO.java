package dao;

import model.Product;
import db_connect.DBManager;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    public void addProduct(Product product) throws SQLException {
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
        String sql = "SELECT * FROM products";

        try (Connection conn = DBManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        }
        return products;
    }

    public void updateProduct(Product product) throws SQLException {
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

    public List<Product> searchProducts(String keyword) throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE name LIKE ? OR description LIKE ? OR manufacturer LIKE ?";

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
