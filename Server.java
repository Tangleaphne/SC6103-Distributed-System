import java.net.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private static final int SERVER_PORT = 12345;

    // 存储航班信息
    private static Map<String, Flight> flights = new HashMap<>();

    static {
        // 在服务器启动时初始化航班信息
        flights.put("FL123", new Flight("FL123", "09:00", 300, 100, "Taipei", "Tokyo"));
        flights.put("FL456", new Flight("FL456", "12:00", 450, 50, "Taipei", "Osaka"));
    }
    public static void main(String[] args) {
        try (DatagramSocket serverSocket = new DatagramSocket(SERVER_PORT)) {
            byte[] receiveBuffer = new byte[1024];
            System.out.println("Server is running...");

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                serverSocket.receive(receivePacket);

                ByteBuffer byteBuffer = ByteBuffer.wrap(receivePacket.getData());
                byte opCode = byteBuffer.get(); // 获取操作码
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();

                if (opCode == 1) {
                    // 查询航班，提取出发地和目的地
                    byte[] srcBytes = new byte[10];
                    byte[] destBytes = new byte[10];
                    byteBuffer.get(srcBytes);
                    byteBuffer.get(destBytes);

                    String src = new String(srcBytes).trim();
                    String dest = new String(destBytes).trim();

                    // 查找符合条件的航班
                    StringBuilder matchingFlights = new StringBuilder();
                    for (Flight flight : flights.values()) {
                        if (flight.src.equals(src) && flight.dest.equals(dest)) {
                            matchingFlights.append(flight.flightId).append(",");
                        }
                    }

                    byte[] sendData;
                    if (matchingFlights.length() > 0) {
                        sendData = ("1," + matchingFlights.toString()).getBytes();
                    } else {
                        sendData = "0".getBytes();  // 无航班匹配
                    }

                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                    serverSocket.send(sendPacket);
                } else if (opCode == 2) {
                    // 查询航班详细信息
                    byte[] flightIdBytes = new byte[10];
                    byteBuffer.get(flightIdBytes);
                    String flightId = new String(flightIdBytes).trim();

                    Flight flight = flights.get(flightId);
                    byte[] sendData;
                    if (flight != null) {
                        sendData = String.format("1,%s,%d,%d", flight.departure, flight.price, flight.seats).getBytes();
                    } else {
                        sendData = "0".getBytes();  // 无该航班
                    }

                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                    serverSocket.send(sendPacket);
                }
                // 其他操作码如座位预订、取消等的处理可扩展实现
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 航班类定义
    static class Flight {
        String flightId;
        String departure;
        int price;
        int seats;
        String src;  // 出发地
        String dest; // 目的地

        // 完整的构造函数，确保所有字段都被初始化
        public Flight(String flightId, String departure, int price, int seats, String src, String dest) {
            this.flightId = flightId;
            this.departure = departure;
            this.price = price;
            this.seats = seats;
            this.src = src;  // 初始化出发地
            this.dest = dest; // 初始化目的地
        }
    }

}
