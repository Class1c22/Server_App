package dao;

import model.Product;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class ProductDAO {

    private final Connection connection;

    public ProductDAO(Connection connection) {
        this.connection = connection;
    }

    public void addProduct(Product product) throws SQLException {
        if (isProductNameExists(product.getName())) {
            throw new SQLException("Product name '" + product.getName() + "' already exists. Product names must be unique.");
        }

        String sql = "INSERT INTO products (name, description, manufacturer, quantity, price_per_unit, group_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
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
        if (isProductNameExistsForOthers(product.getName(), product.getId())) {
            throw new SQLException("Product name '" + product.getName() + "' already exists. Product names must be unique.");
        }

        String sql = "UPDATE products SET name = ?, description = ?, manufacturer = ?, quantity = ?, price_per_unit = ?, group_id = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
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

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public void addStock(int productId, int quantity) throws SQLException {
        String updateSql = "UPDATE products SET quantity = quantity + ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
            stmt.setInt(1, quantity);
            stmt.setInt(2, productId);
            stmt.executeUpdate();
        }
    }

    public void removeStock(int productId, int quantity) throws SQLException {
        Product product = getProductById(productId);
        if (product == null) {
            throw new SQLException("Product not found");
        }

        if (product.getQuantity() < quantity) {
            throw new SQLException("Нема стільки товару на складі. додано: " + product.getQuantity() + ", запрошено: " + quantity);
        }

        String updateSql = "UPDATE products SET quantity = quantity - ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
            stmt.setInt(1, quantity);
            stmt.setInt(2, productId);
            stmt.executeUpdate();
        }
    }

    public Product getProductById(int id) throws SQLException {
        String sql = "SELECT * FROM products WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProduct(rs);
                }
                return null;
            }
        }
    }

    public List<Product> getAllProducts() throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products ORDER BY name";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        }
        return products;
    }



    private boolean isProductNameExists(String name) throws SQLException {
        String sql = "SELECT COUNT(*) FROM products WHERE name = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }
        }
    }
    public void addStock(Connection conn, int productId, int quantity) throws SQLException {
        String sql = "UPDATE products SET quantity = quantity + ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setInt(2, productId);
            stmt.executeUpdate();
        }
    }

    public void removeStock(Connection conn, int productId, int quantity) throws SQLException {
        String sql = "UPDATE products SET quantity = quantity - ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setInt(2, productId);
            stmt.executeUpdate();
        }
    }

    private boolean isProductNameExistsForOthers(String name, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM products WHERE name = ? AND id != ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setInt(2, excludeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }
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
