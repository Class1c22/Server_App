package dao;

import model.ProductGroup;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductGroupDAO {

    private final Connection connection;

    public ProductGroupDAO(Connection connection) {
        this.connection = connection;
    }

    public void addGroup(ProductGroup group) throws SQLException {
        if (isGroupNameExists(group.getName())) {
            throw new SQLException("Group name '" + group.getName() + "' already exists. Group names must be unique.");
        }

        String sql = "INSERT INTO product_groups (name, description) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, group.getName());
            stmt.setString(2, group.getDescription());
            stmt.executeUpdate();
        }
    }

    public ProductGroup getGroupById(int id) throws SQLException {
        String sql = "SELECT * FROM product_groups WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToGroup(rs);
                }
                return null;
            }
        }
    }

    public List<ProductGroup> getAllGroups() throws SQLException {
        List<ProductGroup> groups = new ArrayList<>();
        String sql = "SELECT * FROM product_groups ORDER BY name";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                groups.add(mapResultSetToGroup(rs));
            }
        }
        return groups;
    }

    public void updateGroup(ProductGroup group) throws SQLException {
        if (isGroupNameExistsForOthers(group.getName(), group.getId())) {
            throw new SQLException("Group name '" + group.getName() + "' already exists. Group names must be unique.");
        }

        String sql = "UPDATE product_groups SET name = ?, description = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, group.getName());
            stmt.setString(2, group.getDescription());
            stmt.setInt(3, group.getId());
            stmt.executeUpdate();
        }
    }

    public void deleteGroup(int id) throws SQLException {
        try {
            connection.setAutoCommit(false);

            String deleteProductsSql = "DELETE FROM products WHERE group_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteProductsSql)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }

            String deleteGroupSql = "DELETE FROM product_groups WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteGroupSql)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public List<ProductGroup> searchGroups(String keyword) throws SQLException {
        List<ProductGroup> groups = new ArrayList<>();
        String sql = "SELECT * FROM product_groups WHERE name LIKE ? OR description LIKE ? ORDER BY name";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            String likeKeyword = "%" + keyword + "%";
            stmt.setString(1, likeKeyword);
            stmt.setString(2, likeKeyword);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    groups.add(mapResultSetToGroup(rs));
                }
            }
        }

        return groups;
    }

    private boolean isGroupNameExists(String name) throws SQLException {
        String sql = "SELECT COUNT(*) FROM product_groups WHERE name = ?";

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

    private boolean isGroupNameExistsForOthers(String name, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM product_groups WHERE name = ? AND id != ?";

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

    private ProductGroup mapResultSetToGroup(ResultSet rs) throws SQLException {
        ProductGroup group = new ProductGroup();
        group.setId(rs.getInt("id"));
        group.setName(rs.getString("name"));
        group.setDescription(rs.getString("description"));
        return group;
    }
}
