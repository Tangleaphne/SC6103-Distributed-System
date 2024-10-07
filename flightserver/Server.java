package flightserver;
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
        /* 在服务器启动时初始化航班信息
        flights.put("FL123", new Flight("FL123", "09:00", 300, 100, "Taipei", "Tokyo"));
        flights.put("FL456", new Flight("FL456", "12:00", 450, 50, "Taipei", "Osaka"));
        // 初始化每个航班的预订信息存储
        reservations.put("FL123", new HashMap<>());
        reservations.put("FL456", new HashMap<>());
        */
        flights.put("FL123", new Flight("FL123", "09:00", 300, 100, "Singapore", "Penang"));
        flights.put("FL456", new Flight("FL456", "12:00", 450, 50, "Shanghai", "Taipei"));
        flights.put("FL789", new Flight("FL789", "08:15", 260, 180, "Tokyo", "Beijing"));
        // 初始化每个航班的预订信息存储
        reservations.put("FL123", new HashMap<>());
        reservations.put("FL456", new HashMap<>());
        reservations.put("FL789", new HashMap<>());
    }
    public static void main(String[] args) {
        try (DatagramSocket serverSocket = new DatagramSocket(SERVER_PORT)) {
        //try (DatagramSocket serverSocket = new DatagramSocket(SERVER_PORT, InetAddress.getByName("0.0.0.0"))){
            byte[] receiveBuffer = new byte[1024];
            System.out.println("\nServer is running...\n");

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
                    /*StringBuilder matchingFlights = new StringBuilder();
                    for (Flight flight : flights.values()) {
                        if (flight.src.equals(src) && flight.dest.equals(dest)) {
                            matchingFlights.append(flight.flightId).append(",");
                        }
                    }*/
                    StringBuilder matchingFlights = new StringBuilder();
                    for (Flight flight : flights.values()) {
                        if (flight.src.equals(src) && flight.dest.equals(dest)) {
                            matchingFlights.append(String.format("Your Flight ID: %s, Departure Time: %s, Price: %d, Seat number: %s",
                                    flight.flightId, flight.departure, flight.price, flight.seats));
                        }
                    }

                    byte[] sendData;
                    if (matchingFlights.length() > 0) {
                        //sendData = ("1," + matchingFlights.toString()).getBytes();   
                        sendData = (matchingFlights.toString()).getBytes();                       
                    } else {
                        //sendData = "0".getBytes();  // 无航班匹配
                        sendData = String.format("No such flight based on your departure and destination: %s, %s", src, dest).getBytes();
                    }

                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                    serverSocket.send(sendPacket);
                } else if (opCode == 2) {
                /*     // 查询航班详细信息
                    byte[] flightIdBytes = new byte[10];
                    byteBuffer.get(flightIdBytes);
                    String flightId = new String(flightIdBytes).trim();

                    Flight flight = flights.get(flightId);
                    byte[] sendData;
                    if (flight != null) {
                        sendData = String.format("Your Flight ID: %s, Departure Time: %s, Price: %d, Remaining seats: %s", flight.flightId, flight.departure, flight.price, flight.seats).getBytes();                        
                    } else {
                        //sendData = "0".getBytes();  // 无该航班
                        sendData = String.format("No such flight based on your flight ID: %s", flightId).getBytes();
                    }

                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                    serverSocket.send(sendPacket);   */
                        // 查询航班详情，提取航班ID
                        byte[] flightIdBytesDetail = new byte[10];
                        byteBuffer.get(flightIdBytesDetail);
                        String flightIdDetail = new String(flightIdBytesDetail).trim();
                    
                        Flight flightDetail = flights.get(flightIdDetail);
                        byte[] sendBuffer;
                        if (flightDetail != null) {
                            // 使用 FlightMarshaller 序列化航班对象
                            sendBuffer = FlightMarshaller.marshall(flightDetail);
                        } else {
                            String flightDetailResponse = "Flight not found";
                            sendBuffer = flightDetailResponse.getBytes();
                        }
                    
                        // 发送响应
                        DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, clientAddress, clientPort);
                        serverSocket.send(sendPacket);
                        System.out.println("Sent flight details for flight: " + flightIdDetail + "\n");
                    
                }
                // 其他操作码如座位预订、取消等的处理可扩展实现
                else if (opCode == 3) {  // 假設 opCode 3 是預訂座位的操作碼
                    // 解析航班號和預訂座位數
                    byte[] flightIdBytes = new byte[10];
                    byteBuffer.get(flightIdBytes);  // 獲取航班號
                    int requestedSeats = byteBuffer.getInt();  // 獲取預訂座位數

                    String flightId = new String(flightIdBytes).trim();
                    Flight flight = flights.get(flightId);  // 從 flights map 中找到相應航班

                    byte[] sendData;
                    if (flight == null) {
                        // 如果航班號不存在，返回錯誤訊息
                        //sendData = "0, 航班不存在".getBytes();
                        sendData = String.format("No such flight based on your flight ID: %s", flightId).getBytes();
                    } else if (flight.seats < requestedSeats) {
                        // 如果剩餘座位不足，返回錯誤訊息
                        //sendData = "0, 座位數不足".getBytes();
                        sendData = String.format("Seats are fully booked based on your flight ID: %s. Please choose another flight.", flightId).getBytes();
                    } else {
                        // 生成預訂編號，並存儲預訂信息
                        int reservationId = reservations.get(flightId).size() + 1;
                        reservations.get(flightId).put(reservationId, requestedSeats);

                        // 預訂成功，減少座位數並返回確認訊息
                        flight.seats -= requestedSeats;

                        // Notify all clients monitoring this flight
                        SeatMonitor.notifyClients(flightId, flight.seats);

                        //sendData = String.format("1, 預訂成功, 預訂編號: %d, 剩餘座位數: %d", reservationId, flight.seats).getBytes();
                        sendData = String.format("Seat booking is successful. Reservation ID: %d, remaining seats: %d", reservationId, flight.seats).getBytes();
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
                        //sendData = "0, 航班不存在".getBytes();
                        sendData = String.format("No such flight based on your flight ID: %s", flightId).getBytes();
                    } else {
                        // 獲取航班的預訂座位 Map
                        Map<Integer, Integer> flightReservations = reservations.get(flightId);

                        if (flightReservations == null || !flightReservations.containsKey(reservationId)) {
                            // 如果預訂不存在
                            //sendData = "0, 預訂不存在或已取消".getBytes();
                            sendData = String.format("No such booking based on your flight ID: %s or it has been cancelled", flightId).getBytes();
                        } else {
                            // 更新座位，取消預訂
                            int seatsToRelease = flightReservations.get(reservationId);
                            flight.seats += seatsToRelease;
                            flightReservations.remove(reservationId);
                            //sendData = String.format("1, 取消成功, 剩餘座位數: %d", flight.seats).getBytes();
                            sendData = String.format("Booking ID: %s is successfully cancelled.", reservationId).getBytes();
                        }
                    }

                    // 發送回應給客戶端
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                    serverSocket.send(sendPacket);
                } else if (opCode == 5) { // 添加行李功能
                    byte[] sendData;
                    String flightId = new String(receivePacket.getData(), 1, 10).trim();
                    int reservationId = ByteBuffer.wrap(receivePacket.getData(), 11, 4).getInt();
                    int luggageCount = ByteBuffer.wrap(receivePacket.getData(), 15, 4).getInt();

                    Flight flight = flights.get(flightId);
                    if (flight == null) {
                        sendData = String.format("Flight %s not found.", flightId).getBytes();
                    } else {
                        Map<Integer, Integer> flightReservations = reservations.get(flightId);
                        if (flightReservations == null || !flightReservations.containsKey(reservationId)) {
                            sendData = String.format("No booking with reservation ID: %d.", reservationId).getBytes();
                        } else {
                            int currentLuggageCount = flightReservations.getOrDefault(reservationId, 0);
                            currentLuggageCount += luggageCount;
                            flightReservations.put(reservationId, currentLuggageCount);
                            sendData = String.format("Successfully added %d luggages. Total luggages: %d.", luggageCount, currentLuggageCount).getBytes();
                        }
                    }

                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                    serverSocket.send(sendPacket);
                
                } else if (opCode == 6) {
                    // Register for monitoring seat availability
                    byte[] flightIdBytes = new byte[10];
                    byteBuffer.get(flightIdBytes);
                    String flightId = new String(flightIdBytes).trim();
                    int monitorInterval = byteBuffer.getInt(); // Monitor interval in seconds

                    byte[] sendData;

                    if (!flights.containsKey(flightId)) {
                        // If flight ID does not exist, send an error response to the client
                        sendData = String.format("Error: Flight ID %s does not exist.", flightId).getBytes();
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                        serverSocket.send(sendPacket);
                    } else {
                        // Register the client for monitoring
                        SeatMonitor.registerClient(flightId, clientAddress, clientPort, monitorInterval);
                        System.out.println("Client registered to monitor flight: " + flightId + "\n");
                    }
                } 
                /*else if (opCode == 7) { // Exit 功能
                    System.out.println("Exit command received, shutting down...");
                    break;
                }*/
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Marshalling
    public static class FlightMarshaller {
        public static byte[] marshall(Flight flight) {
            byte[] flightIdBytes = stringToBytes(flight.getFlightId(), 20);
            byte[] departureBytes = stringToBytes(flight.getDeparture(), 20);
            byte[] srcBytes = stringToBytes(flight.getSrc(), 20);  // 添加 src 字段的序列化
            byte[] destinationBytes = stringToBytes(flight.getDest(), 20);
            byte[] priceBytes = intToBytesBigEndian(flight.getPrice());
            byte[] availableSeatsBytes = intToBytesBigEndian(flight.getSeats());
    
            // 现在有 6 个字段，flightId, departure, src, destination, price, availableSeats
            byte[] result = new byte[20 + 20 + 20 + 20 + 4 + 4];
            System.arraycopy(flightIdBytes, 0, result, 0, 20);
            System.arraycopy(departureBytes, 0, result, 20, 20);
            System.arraycopy(srcBytes, 0, result, 40, 20); // 把 src 放到正确的位置
            System.arraycopy(destinationBytes, 0, result, 60, 20);
            System.arraycopy(priceBytes, 0, result, 80, 4);
            System.arraycopy(availableSeatsBytes, 0, result, 84, 4);
    
            return result;
        }
    
        private static byte[] stringToBytes(String str, int length) {
            byte[] bytes = new byte[length];
            for (int i = 0; i < str.length() && i < length; i++) {
                bytes[i] = (byte) str.charAt(i);
            }
            return bytes;
        }
    
        private static byte[] intToBytesBigEndian(int value) {
            byte[] bytes = new byte[4];
            bytes[0] = (byte) (value >> 24);
            bytes[1] = (byte) (value >> 16);
            bytes[2] = (byte) (value >> 8);
            bytes[3] = (byte) value;
            return bytes;
        }
    }    
    
    
        // SeatMonitor class to manage callbacks
        static class SeatMonitor {
            private static Map<String, Map<InetAddress, Integer>> clientCallbacks = new HashMap<>();
    
            public static void registerClient(String flightId, InetAddress clientAddress, int clientPort, int monitorInterval) {
                clientCallbacks.putIfAbsent(flightId, new HashMap<>());
                clientCallbacks.get(flightId).put(clientAddress, clientPort);
    
                new Thread(() -> {
                    try {
                        Thread.sleep(monitorInterval * 1000L);
                        unregisterClient(flightId, clientAddress);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
                System.out.println("Client MONITORING for flight: " + flightId + ", Address: " + clientAddress + ", Port: " + clientPort);
            }
    
            public static void unregisterClient(String flightId, InetAddress clientAddress) {
                Map<InetAddress, Integer> clients = clientCallbacks.get(flightId);
                if (clients != null) {
                    clients.remove(clientAddress);
                    if (clients.isEmpty()) {
                        clientCallbacks.remove(flightId);
                    }
                }
            }
    
            public static void notifyClients(String flightId, int seatsAvailable) {
                Map<InetAddress, Integer> clients = clientCallbacks.get(flightId);
                if (clients != null) {
                    System.out.println("Notifying clients about updated seats for flight: " + flightId + ", Seats remaining: " + seatsAvailable);
                    for (Map.Entry<InetAddress, Integer> entry : clients.entrySet()) {
                        System.out.println("Sending update to client: " + entry.getKey() + ":" + entry.getValue() + "\n");
                        try (DatagramSocket socket = new DatagramSocket()) {
                            String message = String.format("Updated seat availability for flight %s: %d seats remaining", flightId, seatsAvailable);
                            byte[] sendData = message.getBytes();
                            DatagramPacket packet = new DatagramPacket(sendData, sendData.length, entry.getKey(), entry.getValue());
                            socket.send(packet);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

}
