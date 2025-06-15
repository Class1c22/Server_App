import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;
import db_connect.DBManager;
import dao.ProductDAO;
import dao.ProductGroupDAO;
import model.Product;
import model.ProductGroup;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

public class Server {
    private static final int PORT = 8081;
    private static Connection dbConnection;
    private static ProductDAO productDAO;
    private static ProductGroupDAO groupDAO;
    private static Gson gson = new Gson();

    public static void main(String[] args) {
        System.out.println("🔌 HTTP Сервер запускається на порті " + PORT + "...");

        try {
            // Підключення до бази даних
            dbConnection = DBManager.getConnection();
            productDAO = new ProductDAO(dbConnection);
            groupDAO = new ProductGroupDAO(dbConnection);
            System.out.println("✅ Підключення до бази даних успішне");
        } catch (SQLException e) {
            System.out.println("❌ Помилка підключення до бази даних: " + e.getMessage());
            return;
        }

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // Налаштування CORS для всіх запитів
            server.createContext("/", new CORSHandler());

            // API endpoints
            server.createContext("/group", new GetGroupHandler());
            server.createContext("/update-group", new UpdateGroupHandler());
            server.createContext("/groups", new GroupsHandler());
            server.createContext("/products", new ProductsHandler());
            server.createContext("/add-group", new AddGroupHandler());
            server.createContext("/add-product", new AddProductHandler());
            server.createContext("/add-stock", new AddStockHandler());
            server.createContext("/remove-stock", new RemoveStockHandler());
            server.createContext("/delete-group", new DeleteGroupHandler());
            server.createContext("/update-product", new UpdateProductHandler());
            server.createContext("/delete-product", new DeleteProductHandler());

            server.setExecutor(null);
            server.start();

            System.out.println("🌐 HTTP Сервер працює на http://localhost:" + PORT);
            System.out.println("📡 Доступні endpoints:");
            System.out.println("   GET  /groups - отримати всі групи");
            System.out.println("   GET  /products - отримати всі товари");
            System.out.println("   POST /add-group - додати групу");
            System.out.println("   POST /add-product - додати товар");
            System.out.println("   POST /add-stock - поповнити склад");
            System.out.println("   POST /remove-stock - списати товар");
            System.out.println("   DELETE /delete-group - видалити групу");

        } catch (IOException e) {
            System.err.println("❗ Помилка запуску сервера: " + e.getMessage());
        }
    }

    // Базовий CORS handler
    static class CORSHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            // Якщо це не OPTIONS, передаємо далі
            String response = "API Server is running";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    static class GetGroupHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    String query = exchange.getRequestURI().getQuery();
                    String[] params = query.split("&");
                    int groupId = -1;

                    for (String param : params) {
                        String[] keyValue = param.split("=");
                        if (keyValue.length == 2 && "id".equals(keyValue[0])) {
                            groupId = Integer.parseInt(keyValue[1]);
                            break;
                        }
                    }

                    if (groupId == -1) {
                        sendErrorResponse(exchange, "ID групи не вказано");
                        return;
                    }

                    ProductGroup group = groupDAO.getGroupById(groupId);
                    if (group == null) {
                        sendErrorResponse(exchange, "Групу не знайдено");
                        return;
                    }

                    String jsonResponse = gson.toJson(group);
                    exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                    exchange.sendResponseHeaders(200, jsonResponse.getBytes(StandardCharsets.UTF_8).length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
                    }

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Помилка отримання групи: " + e.getMessage());
                }
            } else {
                sendErrorResponse(exchange, "Метод не підтримується");
            }
        }
    }
    static class UpdateProductHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            if ("PUT".equals(exchange.getRequestMethod()) || "POST".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    JsonObject jsonObject = JsonParser.parseString(requestBody).getAsJsonObject();

                    Product product = new Product();
                    product.setId(jsonObject.get("id").getAsInt());
                    product.setName(jsonObject.get("name").getAsString());
                    product.setGroupId(jsonObject.get("groupId").getAsInt());
                    product.setPricePerUnit(new BigDecimal(jsonObject.get("price").getAsString()));

                    if (jsonObject.has("manufacturer") && !jsonObject.get("manufacturer").isJsonNull()) {
                        product.setManufacturer(jsonObject.get("manufacturer").getAsString());
                    }
                    if (jsonObject.has("description") && !jsonObject.get("description").isJsonNull()) {
                        product.setDescription(jsonObject.get("description").getAsString());
                    }

                    productDAO.updateProduct(product);
                    sendSuccessResponse(exchange, "Товар успішно оновлено");

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Помилка оновлення товару: " + e.getMessage());
                }
            }
        }
    }

    static class DeleteProductHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            if ("DELETE".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    JsonObject jsonObject = JsonParser.parseString(requestBody).getAsJsonObject();

                    int productId = jsonObject.get("id").getAsInt();

                    productDAO.deleteProduct(productId);
                    sendSuccessResponse(exchange, "Товар успішно видалено");

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Помилка видалення товару: " + e.getMessage());
                }
            }
        }
    }
    static class UpdateGroupHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            if ("PUT".equals(exchange.getRequestMethod()) || "POST".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    JsonObject jsonObject = JsonParser.parseString(requestBody).getAsJsonObject();

                    ProductGroup group = new ProductGroup();
                    group.setId(jsonObject.get("id").getAsInt());
                    group.setName(jsonObject.get("name").getAsString());

                    if (jsonObject.has("description") && !jsonObject.get("description").isJsonNull()) {
                        group.setDescription(jsonObject.get("description").getAsString());
                    } else {
                        group.setDescription("");
                    }

                    groupDAO.updateGroup(group);
                    sendSuccessResponse(exchange, "Групу успішно оновлено");

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Помилка оновлення групи: " + e.getMessage());
                }
            } else {
                sendErrorResponse(exchange, "Метод не підтримується");
            }
        }
    }

    // Обробник для видалення групи
    static class DeleteGroupHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            if ("DELETE".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    JsonObject jsonObject = JsonParser.parseString(requestBody).getAsJsonObject();

                    int groupId = jsonObject.get("id").getAsInt();

                    groupDAO.deleteGroup(groupId);
                    sendSuccessResponse(exchange, "Групу успішно видалено");

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Помилка видалення групи: " + e.getMessage());
                }
            } else {
                sendErrorResponse(exchange, "Метод не підтримується");
            }
        }
    }

    // Обробник для отримання всіх груп
    static class GroupsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    List<ProductGroup> groups = groupDAO.getAllGroups();
                    String jsonResponse = gson.toJson(groups);

                    exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                    exchange.sendResponseHeaders(200, jsonResponse.getBytes(StandardCharsets.UTF_8).length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
                    }
                } catch (SQLException e) {
                    sendErrorResponse(exchange, "Помилка отримання груп: " + e.getMessage());
                }
            } else {
                sendErrorResponse(exchange, "Метод не підтримується");
            }
        }
    }

    // Обробник для отримання всіх товарів
    static class ProductsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    List<Product> products = productDAO.getAllProducts();
                    String jsonResponse = gson.toJson(products);

                    exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                    exchange.sendResponseHeaders(200, jsonResponse.getBytes(StandardCharsets.UTF_8).length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
                    }
                } catch (SQLException e) {
                    sendErrorResponse(exchange, "Помилка отримання товарів: " + e.getMessage());
                }
            } else {
                sendErrorResponse(exchange, "Метод не підтримується");
            }
        }
    }

    // Обробник для додавання групи
    static class AddGroupHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    JsonObject jsonObject = JsonParser.parseString(requestBody).getAsJsonObject();

                    ProductGroup group = new ProductGroup();
                    group.setName(jsonObject.get("name").getAsString());
                    if (jsonObject.has("description") && !jsonObject.get("description").isJsonNull()) {
                        group.setDescription(jsonObject.get("description").getAsString());
                    }

                    groupDAO.addGroup(group);
                    sendSuccessResponse(exchange, "Групу успішно додано");

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Помилка додавання групи: " + e.getMessage());
                }
            } else {
                sendErrorResponse(exchange, "Метод не підтримується");
            }
        }
    }

    // Обробник для додавання товару
    static class AddProductHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    JsonObject jsonObject = JsonParser.parseString(requestBody).getAsJsonObject();

                    Product product = new Product();
                    product.setName(jsonObject.get("name").getAsString());
                    product.setGroupId(jsonObject.get("groupId").getAsInt());
                    product.setPricePerUnit(new BigDecimal(jsonObject.get("price").getAsString()));

                    if (jsonObject.has("manufacturer") && !jsonObject.get("manufacturer").isJsonNull()) {
                        product.setManufacturer(jsonObject.get("manufacturer").getAsString());
                    }
                    if (jsonObject.has("description") && !jsonObject.get("description").isJsonNull()) {
                        product.setDescription(jsonObject.get("description").getAsString());
                    }

                    // Початкова кількість = 0
                    product.setQuantity(0);

                    productDAO.addProduct(product);
                    sendSuccessResponse(exchange, "Товар успішно додано");

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Помилка додавання товару: " + e.getMessage());
                }
            } else {
                sendErrorResponse(exchange, "Метод не підтримується");
            }
        }
    }

    // Обробник для поповнення складу
    static class AddStockHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    JsonObject jsonObject = JsonParser.parseString(requestBody).getAsJsonObject();

                    int productId = jsonObject.get("productId").getAsInt();
                    int quantity = jsonObject.get("quantity").getAsInt();

                    productDAO.addStock(productId, quantity);
                    sendSuccessResponse(exchange, "Склад успішно поповнено");

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Помилка поповнення складу: " + e.getMessage());
                }
            } else {
                sendErrorResponse(exchange, "Метод не підтримується");
            }
        }
    }

    // Обробник для списання товару
    static class RemoveStockHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    JsonObject jsonObject = JsonParser.parseString(requestBody).getAsJsonObject();

                    int productId = jsonObject.get("productId").getAsInt();
                    int quantity = jsonObject.get("quantity").getAsInt();

                    productDAO.removeStock(productId, quantity);
                    sendSuccessResponse(exchange, "Товар успішно списано");

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Помилка списання товару: " + e.getMessage());
                }
            } else {
                sendErrorResponse(exchange, "Метод не підтримується");
            }
        }
    }

    // Утилітарні методи
    private static void setCORSHeaders(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private static void sendSuccessResponse(HttpExchange exchange, String message) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(200, message.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(message.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void sendErrorResponse(HttpExchange exchange, String message) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(400, message.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(message.getBytes(StandardCharsets.UTF_8));
        }
    }
}