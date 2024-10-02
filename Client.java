import java.net.*;
import java.nio.ByteBuffer;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (DatagramSocket clientSocket = new DatagramSocket()) {
            Scanner scanner = new Scanner(System.in);

            // 顯示選單
            System.out.println("Main menu：");
            System.out.println("1. Query Flight by Source and Destination");
            System.out.println("2. Reserve seats");
            int choice = scanner.nextInt();
            scanner.nextLine(); // 處理換行符

            if (choice == 1) {
                // 查詢航班
                System.out.println("Please enter departure: ");
                String src = scanner.nextLine();
                System.out.println("Please enter destination: ");
                String dest = scanner.nextLine();

                // 构建查询航班的请求
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                byteBuffer.put((byte) 1);  // 操作碼：1表示查询航班
                byteBuffer.put(formatString(src, 10).getBytes());  // 出发地
                byteBuffer.put(formatString(dest, 10).getBytes()); // 目的地

                byte[] sendData = byteBuffer.array();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
                clientSocket.send(sendPacket);

            } else if (choice == 2) {
                // 預訂座位
                System.out.println("Please enter flight ID：");
                String flightId = scanner.nextLine();
                System.out.println("Please enter number of seats to reserve：");
                int seats = scanner.nextInt();

                // 構建預訂座位的請求
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                byteBuffer.put((byte) 3);  // 操作码：3表示预订座位
                byteBuffer.put(formatString(flightId, 10).getBytes());  // 班機號
                byteBuffer.putInt(seats);  // 預訂座位數

                byte[] sendData = byteBuffer.array();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
                clientSocket.send(sendPacket);
            }

            // 接收伺服器的回應
            byte[] receiveBuffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            clientSocket.receive(receivePacket);

            String response = new String(receivePacket.getData()).trim();
            System.out.println("Server Response: " + response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 将字符串格式化为固定长度
    private static String formatString(String input, int length) {
        if (input.length() > length) {
            return input.substring(0, length);
        }
        return String.format("%-" + length + "s", input);
    }
}
