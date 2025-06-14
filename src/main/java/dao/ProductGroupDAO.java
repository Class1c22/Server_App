package dao;

import model.ProductGroup;
import db_connect.DBManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductGroupDAO {

    public void addGroup(ProductGroup group) throws SQLException {
        // Check if group name already exists
        if (isGroupNameExists(group.getName())) {
            throw new SQLException("Group name '" + group.getName() + "' already exists. Group names must be unique.");
        }

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
        String sql = "SELECT * FROM product_groups ORDER BY name";

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
        // Check if group name exists for other groups
        if (isGroupNameExistsForOthers(group.getName(), group.getId())) {
            throw new SQLException("Group name '" + group.getName() + "' already exists. Group names must be unique.");
        }

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
        Connection conn = null;
        try {
            conn = DBManager.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // First delete all products in the group
            String deleteProductsSql = "DELETE FROM products WHERE group_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteProductsSql)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }

            // Then delete the group
            String deleteGroupSql = "DELETE FROM product_groups WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteGroupSql)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }

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

    public List<ProductGroup> searchGroups(String keyword) throws SQLException {
        List<ProductGroup> groups = new ArrayList<>();
        String sql = "SELECT * FROM product_groups WHERE name LIKE ? OR description LIKE ? ORDER BY name";

        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String likeKeyword = "%" + keyword + "%";
            stmt.setString(1, likeKeyword);
            stmt.setString(2, likeKeyword);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                groups.add(mapResultSetToGroup(rs));
            }
        }

        return groups;
    }

    private boolean isGroupNameExists(String name) throws SQLException {
        String sql = "SELECT COUNT(*) FROM product_groups WHERE name = ?";

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

    private boolean isGroupNameExistsForOthers(String name, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM product_groups WHERE name = ? AND id != ?";

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

    private ProductGroup mapResultSetToGroup(ResultSet rs) throws SQLException {
        ProductGroup group = new ProductGroup();
        group.setId(rs.getInt("id"));
        group.setName(rs.getString("name"));
        group.setDescription(rs.getString("description"));
        return group;
    }
}