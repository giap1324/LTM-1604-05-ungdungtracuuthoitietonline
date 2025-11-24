package application;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Lớp TCP Client (Bản thật)
 * Kết nối đến Server Java (localhost:5000)
 */
public class tcpclient {
    
    private Socket socket;
    private BufferedWriter bw;
    private BufferedReader br;
    private String host;
    private int port;

    public tcpclient(String host, int port) {
        this.host = host;
        this.port = port;
        try {
            // Mở kết nối 1 lần và giữ nó
            this.socket = new Socket(host, port);
            this.bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            this.br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            System.out.println("Client đã kết nối đến server " + host + ":" + port);
        } catch (IOException e) {
            System.err.println("❌ Không thể kết nối đến server: " + e.getMessage());
            // Có thể hiển thị lỗi này lên UI, nhưng hiện tại chỉ in ra console
        }
    }
    /**
     * Gửi yêu cầu đồng bộ (cho các lệnh ngắn như ADD_FAV, DEL_FAV)
     */
    public void send(String request) {
        new Thread(() -> {
            if (socket == null || bw == null || br == null) {
                System.err.println("⚠ Không thể gửi yêu cầu - chưa kết nối server");
                return;
            }

            try {
                synchronized (this) {
                    System.out.println("Client gửi lệnh: " + request);
                    bw.write(request);
                    bw.newLine();
                    bw.flush();

                    // Đọc phản hồi đơn giản từ server
                    String response = br.readLine();
                    System.out.println("Server phản hồi: " + response);
                }
            } catch (IOException e) {
                System.err.println("❌ Lỗi khi gửi yêu cầu: " + e.getMessage());
            }
        }).start();
    }


    /**
     * Gửi yêu cầu bất đồng bộ (thật) đến server
     */
    public void sendAsync(String request, Consumer<String> callback) {
        // Chạy trên một luồng mới để không làm đơ UI
        new Thread(() -> {
            // Nếu kết nối ban đầu thất bại, socket sẽ là null
            if (socket == null || bw == null || br == null) {
                callback.accept("{\"cod\":\"503\",\"message\":\"Không thể kết nối đến server\"}");
                return;
            }

            try {
                // Đồng bộ hóa để đảm bảo 2 yêu cầu không bị lẫn lộn
                // (nếu người dùng bấm nút tìm kiếm quá nhanh)
                synchronized (this) {
                    System.out.println("Client gửi: " + request);
                    
                    // 1. Gửi tên thành phố lên Server
                    bw.write(request);
                    bw.newLine();
                    bw.flush();

                    // 2. Chờ và đọc JSON phản hồi từ Server
                    String jsonResponse = br.readLine();
                    
                    System.out.println("Client nhận: " + (jsonResponse != null ? jsonResponse.substring(0, Math.min(jsonResponse.length(), 80)) : "null") + "...");
                    
                    // 3. Gửi JSON về cho client.java để cập nhật UI
                    callback.accept(jsonResponse);
                }

            } catch (IOException e) {
                e.printStackTrace();
                callback.accept(null); // Trả về null nếu có lỗi
            }
        }).start();
    }
    
    
    /**
     * Phương thức đóng kết nối khi ứng dụng tắt
     */
    public void close() {
        System.out.println("Client đóng kết nối.");
        try {
            if (br != null) br.close();
            if (bw != null) bw.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Lỗi khi đóng client: " + e.getMessage());
        }
    }
}