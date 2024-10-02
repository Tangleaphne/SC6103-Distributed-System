import java.net.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private static final int SERVER_PORT = 12345;

    // 存储航班信息
    private static Map<String, Flight> flights = new HashMap<>();
    // 存储每个航班的预订信息
    private static Map<String, Map<Integer, Integer>> reservations = new HashMap<>();

    static {
        flights.put("FL123", new Flight("FL123", "09:00", 300, 100));
        flights.put("FL456", new Flight("FL456", "12:00", 450, 50));
        // 初始化每个航班的预订信息存储
        reservations.put("FL123", new HashMap<>());
        reservations.put("FL456", new HashMap<>());
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
                } else if (opCode == 3) {  // 假設 opCode 3 是預訂座位的操作碼
                    // 解析航班號和預訂座位數
                    byte[] flightIdBytes = new byte[10];
                    byteBuffer.get(flightIdBytes);  // 獲取航班號
                    int requestedSeats = byteBuffer.getInt();  // 獲取預訂座位數

                    String flightId = new String(flightIdBytes).trim();
                    Flight flight = flights.get(flightId);  // 從 flights map 中找到相應航班

                    byte[] sendData;
                    if (flight == null) {
                        // 如果航班號不存在，返回錯誤訊息
                        sendData = "0, 航班不存在".getBytes();
                    } else if (flight.seats < requestedSeats) {
                        // 如果剩餘座位不足，返回錯誤訊息
                        sendData = "0, 座位數不足".getBytes();
                    } else {
                        // 生成預訂編號，並存儲預訂信息
                        int reservationId = reservations.get(flightId).size() + 1;
                        reservations.get(flightId).put(reservationId, requestedSeats);

                        // 預訂成功，減少座位數並返回確認訊息
                        flight.seats -= requestedSeats;
                        sendData = String.format("1, 預訂成功, 預訂編號: %d, 剩餘座位數: %d", reservationId, flight.seats).getBytes();
                    }

                    // 發送回應給客戶端
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                    serverSocket.send(sendPacket);
                } else if (opCode == 4) { // 操作碼 4 是取消座位的操作
                    // 取消預訂操作
                    byte[] flightIdBytes = new byte[10];
                    byteBuffer.get(flightIdBytes);
                    int reservationId = byteBuffer.getInt();  // 獲取預訂編號

                    String flightId = new String(flightIdBytes).trim();
                    Flight flight = flights.get(flightId);

                    byte[] sendData;
                    if (flight == null) {
                        // 如果航班號不存在
                        sendData = "0, 航班不存在".getBytes();
                    } else {
                        // 獲取航班的預訂座位 Map
                        Map<Integer, Integer> flightReservations = reservations.get(flightId);

                        if (flightReservations == null || !flightReservations.containsKey(reservationId)) {
                            // 如果預訂不存在
                            sendData = "0, 預訂不存在或已取消".getBytes();
                        } else {
                            // 更新座位，取消預訂
                            int seatsToRelease = flightReservations.get(reservationId);
                            flight.seats += seatsToRelease;
                            flightReservations.remove(reservationId);
                            sendData = String.format("1, 取消成功, 剩餘座位數: %d", flight.seats).getBytes();
                        }
                    }

                    // 發送回應給客戶端
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                    serverSocket.send(sendPacket);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 航班类
    static class Flight {
        String flightId;
        String departure;
        int price;
        int seats;
        String src;
        String dest;

        public Flight(String flightId, String departure, int price, int seats) {
            this.flightId = flightId;
            this.departure = departure;
            this.price = price;
            this.seats = seats;
        }
    }
}
