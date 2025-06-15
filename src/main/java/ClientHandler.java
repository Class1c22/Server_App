import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.google.gson.Gson;
import dao.ProductDAO;
import dao.ProductGroupDAO;
import model.Product;
import model.ProductGroup;

import java.math.BigDecimal;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Connection dbConnection;
    private final ProductDAO productDAO;
    private final ProductGroupDAO groupDAO;
    private final Gson gson = new Gson();

    public ClientHandler(Socket socket, Connection dbConnection) {
        this.socket = socket;
        this.dbConnection = dbConnection;
        this.productDAO = new ProductDAO(dbConnection);
        this.groupDAO = new ProductGroupDAO(dbConnection);
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if ("exit".equalsIgnoreCase(inputLine)) {
                    out.println("👋 Бувай!");
                    break;
                }

                String[] parts = inputLine.split(";");
                String command = parts[0];

                switch (command) {
                    case "ADD_PRODUCT" -> handleAddProduct(parts, out);
                    case "ADD_GROUP" -> handleAddGroup(parts, out);
                    case "GET_PRODUCTS" -> handleGetProducts(out);
                    case "GET_GROUPS" -> handleGetGroups(out);
                    case "ADD_STOCK" -> handleAddStock(parts, out);
                    case "REMOVE_STOCK" -> handleRemoveStock(parts, out);
                    default -> out.println("⚠️ Невідома команда");
                }
            }

        } catch (IOException e) {
            System.err.println("❗ Помилка при роботі з клієнтом: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("❌ Неможливо закрити сокет");
            }
        }
    }

    private void handleAddProduct(String[] parts, PrintWriter out) {
        if (parts.length == 7) {
            Product product = new Product();
            product.setName(parts[1]);
            product.setDescription(parts[2]);
            product.setManufacturer(parts[3]);
            product.setQuantity(Integer.parseInt(parts[4]));
            product.setPricePerUnit(new BigDecimal(parts[5]));
            product.setGroupId(Integer.parseInt(parts[6]));

            try {
                productDAO.addProduct(product);
                out.println("✅ Товар додано успішно");
            } catch (SQLException e) {
                out.println("❌ Помилка при збереженні товару: " + e.getMessage());
            }
        } else {
            out.println("⚠️ Невірний формат. Використай: ADD_PRODUCT;назва;опис;виробник;кількість;ціна;groupId");
        }
    }

    private void handleAddGroup(String[] parts, PrintWriter out) {
        if (parts.length == 3) {
            ProductGroup group = new ProductGroup();
            group.setName(parts[1]);
            group.setDescription(parts[2]);

            try {
                groupDAO.addGroup(group);
                out.println("✅ Групу додано успішно");
            } catch (SQLException e) {
                out.println("❌ Помилка при збереженні групи: " + e.getMessage());
            }
        } else {
            out.println("⚠️ Невірний формат. Використай: ADD_GROUP;назва;опис");
        }
    }

    private void handleGetProducts(PrintWriter out) {
        try {
            List<Product> products = productDAO.getAllProducts();
            String jsonResponse = gson.toJson(products);
            out.println(jsonResponse);
        } catch (SQLException e) {
            out.println("❌ Помилка при отриманні списку товарів: " + e.getMessage());
        }
    }

    private void handleGetGroups(PrintWriter out) {
        try {
            List<ProductGroup> groups = groupDAO.getAllGroups();
            String jsonResponse = gson.toJson(groups);
            out.println(jsonResponse);
        } catch (SQLException e) {
            out.println("❌ Помилка при отриманні списку груп: " + e.getMessage());
        }
    }

    private void handleAddStock(String[] parts, PrintWriter out) {
        if (parts.length == 3) {
            try {
                int productId = Integer.parseInt(parts[1]);
                int quantity = Integer.parseInt(parts[2]);
                productDAO.addStock(productId, quantity);
                out.println("✅ Склад успішно поповнено");
            } catch (SQLException e) {
                out.println("❌ Помилка при поповненні складу: " + e.getMessage());
            }
        } else {
            out.println("⚠️ Невірний формат. Використай: ADD_STOCK;productId;quantity");
        }
    }

    private void handleRemoveStock(String[] parts, PrintWriter out) {
        if (parts.length == 3) {
            try {
                int productId = Integer.parseInt(parts[1]);
                int quantity = Integer.parseInt(parts[2]);
                productDAO.removeStock(productId, quantity);
                out.println("✅ Товар успішно списано");
            } catch (SQLException e) {
                out.println("❌ Помилка при списанні товару: " + e.getMessage());
            }
        } else {
            out.println("⚠️ Невірний формат. Використай: REMOVE_STOCK;productId;quantity");
        }
    }
}
