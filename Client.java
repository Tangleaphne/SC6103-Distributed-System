import java.net.*;
import java.nio.ByteBuffer;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        /*try (DatagramSocket clientSocket = new DatagramSocket()) {
            Scanner scanner = new Scanner(System.in); */
        try (DatagramSocket clientSocket = new DatagramSocket();
            Scanner scanner = new Scanner(System.in)){
            boolean exit = false;

                while (!exit) {
                    System.out.println("\nMain Menu:");
                    System.out.println("1. Query Flight by Source and Destination");
                    System.out.println("2. Query Flight Details by Flight ID");
                    System.out.println("3. Add new booking");
                    System.out.println("4. Cancel Booking");
                    System.out.println("5. Exit");
                    System.out.print("Enter your choice: ");
                    int choice = scanner.nextInt();
                    scanner.nextLine(); // Consume newline
    
                    switch (choice) 
                    {
                        case 1:

                        System.out.println("Please enter departure: ");
                        String src = scanner.nextLine();
                        System.out.println("Please enter destination: ");
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
                        System.out.println("Server response: " + response);
                        break;

                        case 2:
                        System.out.println("Please enter flight ID: ");
                        String flightId = scanner.nextLine();

                        // 构建请求数据
                        byteBuffer = ByteBuffer.allocate(1024);
                        byteBuffer.put((byte) 2);  // 操作码：2表示查询航班详细信息
                        byteBuffer.put(formatString(flightId, 10).getBytes());

                        sendData = byteBuffer.array();

                        // 发送请求
                        sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
                        clientSocket.send(sendPacket);

                        // 接收响应
                        receiveBuffer = new byte[1024];
                        receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                        clientSocket.receive(receivePacket);

                        response = new String(receivePacket.getData()).trim();
                        System.out.println("Server response: " + response);
                        break;
                        
                        case 3:
                        // 預訂座位
                        System.out.println("Please enter flight ID: ");
                        //String flightId = scanner.nextLine();
                        flightId = scanner.nextLine();
                        System.out.println("Please enter number of seats: ");
                        int seats = scanner.nextInt();

                        // 構建預訂座位的請求
                        //ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                        byteBuffer = ByteBuffer.allocate(1024);
                        byteBuffer.put((byte) 3);  // 操作码：3表示预订座位
                        byteBuffer.put(formatString(flightId, 10).getBytes());  // 班機號
                        byteBuffer.putInt(seats);  // 預訂座位數

                        //byte[] sendData = byteBuffer.array();
                        sendData = byteBuffer.array();
                        //DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
                        sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
                        clientSocket.send(sendPacket);

                        // 接收伺服器的回應
                        //byte[] receiveBuffer = new byte[1024];
                        receiveBuffer = new byte[1024];
                        //DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                        receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                        clientSocket.receive(receivePacket);

                        //String response = new String(receivePacket.getData()).trim();
                        response = new String(receivePacket.getData()).trim();
                        System.out.println("Server response: " + response);
                        break;

                        case 4:
                        // 取消預訂
                        System.out.println("Please enter flight ID: ");
                        //String flightId = scanner.nextLine();
                        flightId = scanner.nextLine();
                        System.out.println("Please enter reservation ID:");
                        int reservationId = scanner.nextInt();

                        // 構建取消預訂的請求
                        //ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                        byteBuffer = ByteBuffer.allocate(1024);
                        byteBuffer.put((byte) 4);  // 操作码：4表示取消預訂
                        byteBuffer.put(formatString(flightId, 10).getBytes());  // 班機號
                        byteBuffer.putInt(reservationId);  // 預訂編號

                        //byte[] sendData = byteBuffer.array();
                        sendData = byteBuffer.array();
                        //DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
                        sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
                        clientSocket.send(sendPacket);

                        // 接收伺服器的回應
                        //byte[] receiveBuffer = new byte[1024];
                        receiveBuffer = new byte[1024];
                        //DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                        receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                        clientSocket.receive(receivePacket);

                        //String response = new String(receivePacket.getData()).trim();
                        response = new String(receivePacket.getData()).trim();
                        System.out.println("Server response: " + response);
                        break;

                        case 5:
                        exit = true;
                        break;

                        default:
                            System.out.println("Invalid choice. Please try again.");
                    }
            }
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
