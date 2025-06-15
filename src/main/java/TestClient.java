import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class TestClient {
    public static void main(String[] args) {
        try {
            // Тестуємо отримання груп
            System.out.println("Отримуємо список груп:");
            String groupsResponse = sendGetRequest("http://localhost:8080/groups");
            System.out.println(groupsResponse);

            // Тестуємо додавання групи
            System.out.println("\nДодаємо нову групу:");
            String newGroup = "{\"name\":\"Нова група\",\"description\":\"Опис нової групи\"}";
            String addGroupResponse = sendPostRequest("http://localhost:8080/add-group", newGroup);
            System.out.println(addGroupResponse);

            // Знову отримуємо групи, щоб побачити оновлений список
            System.out.println("\nОновлений список груп:");
            groupsResponse = sendGetRequest("http://localhost:8080/groups");
            System.out.println(groupsResponse);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String sendGetRequest(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return response.toString();
        } else {
            return "GET request failed. Response Code: " + responseCode;
        }
    }

    private static String sendPostRequest(String urlString, String jsonInput) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInput.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return response.toString();
        } else {
            return "POST request failed. Response Code: " + responseCode;
        }
    }
}