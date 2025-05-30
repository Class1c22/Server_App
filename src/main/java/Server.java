import db_connect.DBManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;

public class Server {

    private static final int PORT = 12345;

    public static void main(String[] args) {
        System.out.println("🔌 Сервер запускається...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✅ Сервер працює на порті " + PORT);

            Connection dbConnection;
            try {
                dbConnection = DBManager.getConnection();
            } catch (SQLException e) {
                System.out.println("❌ Помилка підключення до бази даних: " + e.getMessage());
                return;
            }

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("📥 Підключився клієнт: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket, dbConnection);
                new Thread(handler).start();
            }

        } catch (IOException e) {
            System.err.println("❗ Помилка сервера: " + e.getMessage());
        }
    }
}
