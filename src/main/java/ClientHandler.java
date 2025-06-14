import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
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

    public ClientHandler(Socket socket, Connection dbConnection) {
        this.socket = socket;
        this.dbConnection = dbConnection;
        this.productDAO = new ProductDAO();
        this.groupDAO = new ProductGroupDAO();
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            out.println("👋 Сервер каже: Привіт!");

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if ("exit".equalsIgnoreCase(inputLine)) {
                    out.println("👋 Бувай!");
                    break;
                }

                if (inputLine.startsWith("ADD_PRODUCT")) {
                    // Очікуємо формат: ADD_PRODUCT;назва;опис;виробник;кількість;ціна;groupId
                    String[] parts = inputLine.split(";");
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
                } else if (inputLine.startsWith("ADD_GROUP")) {
                    // Формат: ADD_GROUP;назва;опис
                    String[] parts = inputLine.split(";");
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
                } else {
                    out.println("📨 Ти сказав: " + inputLine);
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

}