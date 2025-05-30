package dao;

import model.ProductGroup;
import db_connect.DBManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductGroupDAO {

    public void addGroup(ProductGroup group) throws SQLException {
        String sql = "INSERT INTO product_groups (name, description) VALUES (?, ?)";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, group.getName());
            stmt.setString(2, group.getDescription());
            stmt.executeUpdate();
        }
    }

    public ProductGroup getGroupById(int id) throws SQLException {
        String sql = "SELECT * FROM product_groups WHERE id = ?";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToGroup(rs);
            }
            return null;
        }
    }

    public List<ProductGroup> getAllGroups() throws SQLException {
        List<ProductGroup> groups = new ArrayList<>();
        String sql = "SELECT * FROM product_groups";

        try (Connection conn = DBManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                groups.add(mapResultSetToGroup(rs));
            }
        }
        return groups;
    }

    public void updateGroup(ProductGroup group) throws SQLException {
        String sql = "UPDATE product_groups SET name = ?, description = ? WHERE id = ?";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, group.getName());
            stmt.setString(2, group.getDescription());
            stmt.setInt(3, group.getId());
            stmt.executeUpdate();
        }
    }

    public void deleteGroup(int id) throws SQLException {
        String sql = "DELETE FROM product_groups WHERE id = ?";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private ProductGroup mapResultSetToGroup(ResultSet rs) throws SQLException {
        ProductGroup group = new ProductGroup();
        group.setId(rs.getInt("id"));
        group.setName(rs.getString("name"));
        group.setDescription(rs.getString("description"));
        return group;
    }
}
