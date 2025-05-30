import java.io.*;
import java.net.Socket;
import java.sql.Connection;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Connection dbConnection;

    public ClientHandler(Socket socket, Connection dbConnection) {
        this.socket = socket;
        this.dbConnection = dbConnection;
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

                out.println("📨 Ти сказав: " + inputLine);
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
