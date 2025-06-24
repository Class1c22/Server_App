import dao.ProductGroupDAO;
import model.ProductGroup;
import org.junit.jupiter.api.*;
import db_connect.DBManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductGroupDAOTest {

    static Connection connection;
    private static ProductGroupDAO groupDAO;

    @BeforeAll
    static void setup() throws SQLException {
        connection = DBManager.getConnection();
        groupDAO = new ProductGroupDAO(connection);

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM products");
            statement.executeUpdate("DELETE FROM product_groups");
        }
    }

    @Test
    @Order(1)
    void testAddGroup() throws SQLException {
        ProductGroup group = new ProductGroup();
        group.setName("TestGroup1");
        group.setDescription("Test Description");

        groupDAO.addGroup(group);

        List<ProductGroup> groups = groupDAO.getAllGroups();
        assertFalse(groups.isEmpty(), "Група повинна бути додана");

        ProductGroup fetched = groups.stream()
                .filter(g -> g.getName().equals("TestGroup1"))
                .findFirst()
                .orElse(null);

        assertNotNull(fetched, "Групу не знайдено");
        assertEquals("Test Description", fetched.getDescription(), "Опис групи не збігається");
    }

    @Test
    @Order(2)
    void testAddGroupWithDuplicateName() throws SQLException {
        ProductGroup group = new ProductGroup();
        group.setName("TestGroup1");
        group.setDescription("Duplicate Group");

        SQLException exception = assertThrows(SQLException.class, () -> groupDAO.addGroup(group));
        assertNotNull(exception, "Очікувалось виникнення SQLException при додаванні дублікату");
    }

    @Test
    @Order(3)
    void testUpdateGroup() throws SQLException {
        ProductGroup group = groupDAO.getAllGroups().get(0);
        group.setDescription("Updated Description");

        groupDAO.updateGroup(group);

        ProductGroup updated = groupDAO.getGroupById(group.getId());
        assertNotNull(updated, "Оновлену групу не знайдено");
        assertEquals("Updated Description", updated.getDescription(), "Опис групи не було оновлено");
    }

    @Test
    @Order(4)
    void testUpdateGroupWithDuplicateName() throws SQLException {
        ProductGroup group2 = new ProductGroup();
        group2.setName("TestGroup2");
        group2.setDescription("Second Group");
        groupDAO.addGroup(group2);

        ProductGroup groupToUpdate = groupDAO.getAllGroups().stream()
                .filter(g -> g.getName().equals("TestGroup2"))
                .findFirst()
                .orElse(null);

        assertNotNull(groupToUpdate, "Групу для оновлення не знайдено");

        groupToUpdate.setName("TestGroup1");

        SQLException exception = assertThrows(SQLException.class, () -> groupDAO.updateGroup(groupToUpdate));
        assertNotNull(exception, "Очікувалось виникнення SQLException при оновленні на існуючу назву");
    }

    @Test
    @Order(5)
    void testSearchGroups() throws SQLException {
        List<ProductGroup> searchResult = groupDAO.searchGroups("TestGroup1");
        assertFalse(searchResult.isEmpty(), "Пошук повинен знайти хоча б одну групу");

        boolean matchFound = searchResult.stream()
                .anyMatch(g -> g.getName().contains("TestGroup1") || g.getDescription().contains("TestGroup1"));

        assertTrue(matchFound, "Група з назвою або описом, що містить 'TestGroup1', не знайдена");
    }

    @Test
    @Order(6)
    void testDeleteGroup() throws SQLException {
        ProductGroup group = new ProductGroup();
        group.setName("DeleteGroup");
        group.setDescription("Group to delete");
        groupDAO.addGroup(group);

        ProductGroup addedGroup = groupDAO.getAllGroups().stream()
                .filter(g -> g.getName().equals("DeleteGroup"))
                .findFirst()
                .orElse(null);

        assertNotNull(addedGroup, "Групу для видалення не знайдено");

        groupDAO.deleteGroup(addedGroup.getId());

        ProductGroup deletedGroup = groupDAO.getGroupById(addedGroup.getId());
        assertNull(deletedGroup, "Група не була видалена");
    }

    @AfterAll
    static void teardown() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM products");
            statement.executeUpdate("DELETE FROM product_groups");
        }
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
