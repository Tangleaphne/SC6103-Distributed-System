package flightserver;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Scanner;
import java.util.InputMismatchException;


public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    //private static final String SERVER_ADDRESS = "0.tcp.ap.ngrok.io";

    private static final int SERVER_PORT = 12345;
    //private static final int SERVER_PORT = 14414;

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
                    System.out.println("5. Add Luggage");  // 修改點：新增添加行李功能
                    System.out.println("6. Monitor Seat Availability");
                    System.out.println("7. Exit");
                    System.out.print("Enter your choice: ");

                    try {
                        if (scanner.hasNextInt()) {
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
                            String flightIdDetail = scanner.nextLine();
                            byteBuffer = ByteBuffer.allocate(1024);
                            byteBuffer.put((byte) 2);
                            byteBuffer.put(formatString(flightIdDetail, 10).getBytes());
                            sendData = byteBuffer.array();

                            sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
                            clientSocket.send(sendPacket);

                            // 接收服务器的响应
                            byte[] receiveData = new byte[1024];
                            receivePacket = new DatagramPacket(receiveData, receiveData.length);
                            clientSocket.receive(receivePacket);

                            // 反序列化接收到的数据为 Flight 对象
                            Flight flightDetail = FlightUnmarshaller.unmarshall(receivePacket.getData());
                            System.out.println("Received Flight details: " + flightDetail);
                            break;

                        
                        case 3:
                        // 預訂座位
                        System.out.println("Please enter flight ID: ");
                        String flightId = scanner.nextLine();
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

                        case 5: // 添加行李功能
                        System.out.println("Please enter your flight ID: ");
                        //String flightId = scanner.nextLine();
                        flightId = scanner.nextLine();
                        System.out.println("Please enter your reservation ID: ");
                        //int reservationId = scanner.nextInt();
                        reservationId = scanner.nextInt();
                        System.out.println("Please enter the number of luggages to add: ");
                        int luggageCount = scanner.nextInt();

                        //ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                        byteBuffer = ByteBuffer.allocate(1024);
                        byteBuffer.put((byte) 5);  // 操作碼：5表示添加行李
                        byteBuffer.put(formatString(flightId, 10).getBytes());
                        byteBuffer.putInt(reservationId);
                        byteBuffer.putInt(luggageCount);
                        //byte[] sendData = byteBuffer.array();
                        sendData = byteBuffer.array();

                        //DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
                        sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
                        clientSocket.send(sendPacket);

                        // 等待伺服器回應
                        receiveData = new byte[1024];
                        //DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        clientSocket.receive(receivePacket);
                        //String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                        response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                        
                        System.out.println("Server response: " + response);

                        break;

                        case 6:
                        System.out.println("Please enter flight ID to monitor: ");
                        //String flightId = scanner.nextLine();
                        flightId = scanner.nextLine();
                        System.out.println("Please enter monitor interval in seconds: ");
                        int monitorInterval = scanner.nextInt();

                        //ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                        byteBuffer = ByteBuffer.allocate(1024);
                        byteBuffer.put((byte) 6);  // 操作码：6表示监控座位
                        byteBuffer.put(formatString(flightId, 10).getBytes());
                        byteBuffer.putInt(monitorInterval);

                        //byte[] sendData = byteBuffer.array();
                        sendData = byteBuffer.array();
                        //DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
                        sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
                        clientSocket.send(sendPacket);
                        System.out.println("Registered for seat availability updates for flight: " + flightId);

                        // Set a timeout for receiving updates from the server
                        clientSocket.setSoTimeout((monitorInterval + 1) * 1000); // Set timeout slightly longer than monitor interval

                        try {
                            // Listen for a single update from the server
                            byte[] localReceiveBuffer = new byte[1024];
                            DatagramPacket localReceivePacket = new DatagramPacket(localReceiveBuffer, localReceiveBuffer.length);
                            clientSocket.receive(localReceivePacket); // Wait for an update or timeout
                            String localResponse = new String(localReceivePacket.getData(), 0, localReceivePacket.getLength()).trim();
                            System.out.println("Update received from server: " + localResponse);
                        } catch (SocketTimeoutException e) {
                            System.out.printf("No updates received within the interval %d seconds. Returning to main menu.\n", monitorInterval);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        break;

                        case 7:
                        System.out.println("Exit command received, shutting down... Have a nice day!\n");
                        exit = true;
                        break;

                        default:
                            System.out.println("Invalid choice. Please try again.");
                        }
                
                    } else {
                        System.out.println("Invalid input. Please enter a valid number.");
                        scanner.next(); // Consume the invalid input
                    }
                    } catch (InputMismatchException e) {
                        System.out.println("Invalid input. Please enter a valid number.");
                        scanner.nextLine(); // Clear the invalid input from the scanner
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

    public static class FlightUnmarshaller {
        public static Flight unmarshall(byte[] data) {
            String flightId = bytesToString(data, 0, 20);
            String departure = bytesToString(data, 20, 20);
            String src = bytesToString(data, 40, 20);    // 反序列化 src 字段
            String dest = bytesToString(data, 60, 20);
            int price = ByteBuffer.wrap(data, 80, 4).getInt();
            int seats = ByteBuffer.wrap(data, 84, 4).getInt();
    
            return new Flight(flightId, departure, price, seats, src, dest);
        }
    
        private static String bytesToString(byte[] data, int offset, int length) {
            StringBuilder str = new StringBuilder();
            for (int i = offset; i < offset + length; i++) {
                if (data[i] != 0) {
                    str.append((char) data[i]);
                }
            }
            return str.toString().trim();
        }
    }    

}
