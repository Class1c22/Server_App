import java.io.*;
import java.net.Socket;

public class TestClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 12345);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))
        ) {
            System.out.println(in.readLine()); // Привітання сервера

            String input;
            while ((input = userInput.readLine()) != null) {
                out.println(input); // Надсилаємо серверу
                System.out.println(in.readLine()); // Читаємо відповідь
                if ("exit".equalsIgnoreCase(input)) break;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
