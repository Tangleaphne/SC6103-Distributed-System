package flightserver;

import java.net.*;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.Scanner;
import java.util.InputMismatchException;
import java.nio.charset.StandardCharsets;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    // private static final String SERVER_ADDRESS = "10.91.61.102";
    // 10.91.61.102
    private static final int SERVER_PORT = 12345;
    private static final int MAX_RETRIES = 5;

    public static void main(String[] args) {
        try (DatagramSocket clientSocket = new DatagramSocket();
                Scanner scanner = new Scanner(System.in)) {
            boolean exit = false;

            while (!exit) {
                System.out.println("\nMain Menu:");
                System.out.println("1. Query Flight by Source and Destination");
                System.out.println("2. Query Flight Details by Flight ID");
                System.out.println("3. Add new booking");
                System.out.println("4. Cancel Booking");
                System.out.println("5. Add Luggage");
                System.out.println("6. Monitor Seat Availability");
                System.out.println("7. Check Server Time");
                System.out.println("8. Exit");
                System.out.print("Enter your choice: ");

                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                byte[] sendData;
                try {
                    if (scanner.hasNextInt()) {
                        int choice = scanner.nextInt();
                        scanner.nextLine(); // Consume newline

                        switch (choice) {
                            case 1:
                                System.out.println("Please enter departure: ");
                                String src = scanner.nextLine();
                                System.out.println("Please enter destination: ");
                                String dest = scanner.nextLine();

                                // 构建请求数据
                                byteBuffer.put((byte) 1); // 操作码：1表示查询航班
                                byteBuffer.put(formatString(src, 10).getBytes()); // 出发地
                                byteBuffer.put(formatString(dest, 10).getBytes()); // 目的地

                                sendData = byteBuffer.array();

                                // 模拟訊息丟失並發送請求
                                sendWithPotentialLoss(clientSocket, sendData);
                                break;

                            case 2:
                                System.out.println("Please enter flight ID: ");
                                String flightId = scanner.nextLine();

                                // 构建请求数据
                                // byteBuffer.clear();
                                byteBuffer = ByteBuffer.allocate(1024);
                                byteBuffer.put((byte) 2); // 操作码：2表示查询航班详细信息
                                byteBuffer.put(formatString(flightId, 10).getBytes());

                                sendData = byteBuffer.array();

                                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                                        InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
                                clientSocket.send(sendPacket);

                                // 接收服务器的响应
                                byte[] receiveData = new byte[1024];
                                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                                clientSocket.receive(receivePacket);

                                // 反序列化接收到的数据为 Flight 对象
                                Flight flightDetail = FlightUnmarshaller.unmarshall(receivePacket.getData());
                                System.out.println("Received Flight details: " + flightDetail);

                                // 模拟訊息丟失並發送請求
                                // sendWithPotentialLoss(clientSocket, sendData);
                                break;

                            case 3:
                                System.out.println("Please enter flight ID: ");
                                flightId = scanner.nextLine();
                                System.out.println("Please enter number of seats: ");
                                int seats = scanner.nextInt();

                                // 構建預訂座位的請求
                                byteBuffer.clear();
                                byteBuffer.put((byte) 3); // 操作码：3表示预订座位
                                byteBuffer.put(formatString(flightId, 10).getBytes());
                                byteBuffer.putInt(seats);

                                sendData = byteBuffer.array();
                                sendWithPotentialLoss(clientSocket, sendData);
                                System.out.println("Client is sending flight ID: '" + flightId + "'");
                                break;

                            case 4:
                                System.out.println("Please enter flight ID: ");
                                flightId = scanner.nextLine();
                                System.out.println("Please enter reservation ID:");
                                int reservationId = scanner.nextInt();

                                // 構建取消預訂的請求
                                byteBuffer.clear();
                                byteBuffer.put((byte) 4); // 操作码：4表示取消預訂
                                byteBuffer.put(formatString(flightId, 10).getBytes());
                                byteBuffer.putInt(reservationId);

                                sendData = byteBuffer.array();
                                sendWithPotentialLoss(clientSocket, sendData);
                                break;

                            case 5:
                                System.out.println("Please enter your flight ID: ");
                                flightId = scanner.nextLine();
                                System.out.println("Please enter your reservation ID: ");
                                reservationId = scanner.nextInt();
                                System.out.println("Please enter the number of luggages to add: ");
                                int luggageCount = scanner.nextInt();

                                // 構建添加行李的請求
                                byteBuffer.clear();
                                byteBuffer.put((byte) 5); // 操作码：5表示添加行李
                                byteBuffer.put(formatString(flightId, 10).getBytes());
                                byteBuffer.putInt(reservationId);
                                byteBuffer.putInt(luggageCount);

                                sendData = byteBuffer.array();
                                sendWithPotentialLoss(clientSocket, sendData);
                                break;

                            case 6:
                                System.out.println("Please enter flight ID to monitor: ");
                                flightId = scanner.nextLine();
                                System.out.println("Please enter monitor interval in seconds: ");
                                int monitorInterval = scanner.nextInt();

                                byteBuffer.clear();
                                byteBuffer.put((byte) 6); // 操作码：6表示监控座位
                                byteBuffer.put(formatString(flightId, 10).getBytes());
                                byteBuffer.putInt(monitorInterval);

                                sendData = byteBuffer.array();
                                sendWithPotentialLoss(clientSocket, sendData);

                                // 設置接收更新的超時
                                clientSocket.setSoTimeout((monitorInterval + 1) * 800); // 超时比监控间隔稍长

                                try {
                                    // 等待伺服器更新或超時
                                    byte[] localReceiveBuffer = new byte[1024];
                                    DatagramPacket localReceivePacket = new DatagramPacket(localReceiveBuffer,
                                            localReceiveBuffer.length);
                                    clientSocket.receive(localReceivePacket);
                                    String localResponse = new String(localReceivePacket.getData(), 0,
                                            localReceivePacket.getLength()).trim();
                                    System.out.println("Update received from server: " + localResponse);
                                } catch (SocketTimeoutException e) {
                                    System.out.printf(
                                            "No updates received within the interval %d seconds. Returning to main menu.\n",
                                            monitorInterval);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                break;

                            case 7:
                                // 构建请求数据
                                byteBuffer.put((byte) 7); // 操作码：7表示获取服务器时间

                                sendData = byteBuffer.array();

                                sendPacket = new DatagramPacket(sendData, sendData.length,
                                        InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
                                clientSocket.send(sendPacket);

                                // 等待服务器返回当前时间
                                byte[] receiveBuffer = new byte[1024];
                                receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                                clientSocket.receive(receivePacket);
                                String serverTime = new String(receivePacket.getData()).trim();
                                System.out.println("Server time: " + serverTime);
                                break;

                            case 8:
                                System.out.println("Thanks for using our service. Have a nice day!\n");
                                exit = true;
                                break;

                            default:
                                System.out.println("Invalid choice.");
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

    // 模擬訊息丟失並且嘗試重傳
    private static void sendWithPotentialLoss(DatagramSocket socket, byte[] sendData) {
        Random random = new Random();
        boolean acknowledged = false;
        int retryCount = 0;

        while (!acknowledged && retryCount < MAX_RETRIES) {
            System.out.println("goooood0");
            if (random.nextInt(10) > 0) { // 90% 機率發送訊息
                System.out.println("goooood0.1");
                try {
                    socket.setSoTimeout(20000); // 20秒超时
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                            InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
                    socket.send(sendPacket);
                    System.out.println("Request sent: " + new String(sendData, StandardCharsets.UTF_8).trim() + "\n");
                    System.out.println("goooood1");

                    byte[] receiveBuffer = new byte[1024]; // 這裡開始好像不會被經過就直接到 timeout
                    System.out.println("goooood5");
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    System.out.println("goooood3");
                    socket.setSoTimeout(20000); // 20秒超时
                    System.out.println("goooood4");
                    socket.receive(receivePacket); // 沒設置 timeout 的話這裡就會永遠卡死
                    String response = new String(receivePacket.getData()).trim();
                    System.out.println("Server response: \n" + response);
                    acknowledged = true;
                    System.out.println("goooood2");
                } catch (SocketTimeoutException e) {
                    System.out.println("Timeout, retrying...");
                    retryCount++;
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("goooood3");
                }
            } else {
                System.out.println("Simulated message loss, request not sent.");
                retryCount++;
            }
        }

        if (!acknowledged) {
            System.out.println("Request failed after retries.");
        }
    }

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
            String src = bytesToString(data, 40, 20); // 反序列化 src 字段
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
