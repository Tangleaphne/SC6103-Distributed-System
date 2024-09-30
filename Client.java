import java.net.*;
import java.nio.ByteBuffer;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (DatagramSocket clientSocket = new DatagramSocket()) {
            Scanner scanner = new Scanner(System.in);

            System.out.println("请输入出发地: ");
            String src = scanner.nextLine();
            System.out.println("请输入目的地: ");
            String dest = scanner.nextLine();

            // 构建请求数据
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            byteBuffer.put((byte) 1);  // 操作码：1表示查询航班
            byteBuffer.put(formatString(src, 10).getBytes());  // 出发地
            byteBuffer.put(formatString(dest, 10).getBytes()); // 目的地

            byte[] sendData = byteBuffer.array();

            // 发送查询请求
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
            clientSocket.send(sendPacket);

            // 接收响应
            byte[] receiveBuffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            clientSocket.receive(receivePacket);

            String response = new String(receivePacket.getData()).trim();
            System.out.println("服务器响应: " + response);
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
