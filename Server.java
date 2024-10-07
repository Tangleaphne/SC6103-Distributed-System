import java.net.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.io.IOException;

public class Server {
    private static final int SERVER_PORT = 12345;

    // 存储航班信息
    private static Map<String, Flight> flights = new HashMap<>();
    // 存储每个航班的预订信息
    private static Map<String, Map<Integer, Integer>> reservations = new HashMap<>();
    // 存储已处理的请求以实现 at-most-once 语义
    private static Map<String, Boolean> processedRequests = new HashMap<>();

    static {
        // 初始化航班信息
        flights.put("FL123", new Flight("FL123", "09:00", 300, 100, "Taipei", "Tokyo"));
        flights.put("FL456", new Flight("FL456", "12:00", 450, 50, "Taipei", "Osaka"));
        reservations.put("FL123", new HashMap<>());
        reservations.put("FL456", new HashMap<>());
    }

    public static void main(String[] args) {
        
        try (DatagramSocket serverSocket = new DatagramSocket(SERVER_PORT)) {
            byte[] receiveBuffer = new byte[1024];
            System.out.println("\nServer is running...\n");

            Map<String, CachedResponse> processedRequests = new HashMap<>();

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                serverSocket.receive(receivePacket);
                ByteBuffer byteBuffer = ByteBuffer.wrap(receivePacket.getData());
                byte opCode = byteBuffer.get(); // 获取操作码
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                String requestID = clientAddress.toString() + clientPort + byteBuffer.toString(); // 唯一请求ID

                /*  防止重复处理相同请求
                if (processedRequests.containsKey(requestID)) {
                    System.out.println("Duplicate request, ignoring.");
                    continue;
                }*/

                // 檢查是否有最近的回應 (3 秒內)
                long currentTime = System.currentTimeMillis();
                if (processedRequests.containsKey(requestID)) {
                    CachedResponse cachedResponse = processedRequests.get(requestID);
                    if (currentTime - cachedResponse.timestamp < 3000) {
                        // 在 3 秒內，使用上次的回應結果
                        System.out.println("Returning cached response for request: " + requestID);
                        sendResponse(serverSocket, cachedResponse.data, clientAddress, clientPort);
                        continue;
                    }
                }

                // 處理請求並獲取新的回應數據
                byte[] sendData;
                switch (opCode) {
                    case 1:
                        sendData = processFlightQuery(byteBuffer);
                        break;
                        
                    case 2:
                        sendData = processFlightDetails(byteBuffer);
                        break;
                        
                    case 3:
                        sendData = processBooking(byteBuffer);
                        break;

                    case 4:
                        sendData = processCancellation(byteBuffer);
                        break;

                    case 5:
                        sendData = processLuggage(byteBuffer);
                        break;

                    case 6:
                        sendData = registerMonitor(byteBuffer, clientAddress, clientPort);
                        break;

                    default:
                        sendData = "Unknown operation.".getBytes();
                }

                // 更新緩存回應
                processedRequests.put(requestID, new CachedResponse(sendData, currentTime));


                // 模拟消息丢失
                Random random = new Random();
                if (random.nextInt(10) > 1) {
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                    serverSocket.send(sendPacket);
                    System.out.println("Response sent.");
                    processedRequests.put(requestID, new CachedResponse(sendData, currentTime));  // 请求已处理
                } else {
                    System.out.println("Simulated message loss, response not sent.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // case1 定義 processFlightQuery 方法在 main 方法之後
    private static byte[] processFlightQuery(ByteBuffer byteBuffer) {
        byte[] srcBytes = new byte[10];
        byte[] destBytes = new byte[10];
        byteBuffer.get(srcBytes);
        byteBuffer.get(destBytes);
        String src = new String(srcBytes).trim();
        String dest = new String(destBytes).trim();

        StringBuilder matchingFlights = new StringBuilder();
        for (Flight flight : flights.values()) {
            if (flight.src.equals(src) && flight.dest.equals(dest)) {
                matchingFlights.append(String.format("Flight ID: %s, Departure: %s, Price: %d, Seats: %d",
                        flight.flightId, flight.departure, flight.price, flight.seats));
            }
        }

        if (matchingFlights.length() > 0) {
            return matchingFlights.toString().getBytes();
        } else {
            return String.format("No flights from %s to %s.", src, dest).getBytes();
        }
    }
    
    // case2 定義 processFlightDetails 方法
    private static byte[] processFlightDetails(ByteBuffer byteBuffer) {
        byte[] flightIdBytes = new byte[10];
        byteBuffer.get(flightIdBytes);
        String flightId = new String(flightIdBytes).trim();
        Flight flight = flights.get(flightId);

        if (flight != null) {
            return String.format("Flight ID: %s, Departure: %s, Price: %d, Seats: %d",
                    flight.flightId, flight.departure, flight.price, flight.seats).getBytes();
        } else {
            return "Flight not found.".getBytes();
        }
    }

    // case3 定義 processBooking 方法
    private static byte[] processBooking(ByteBuffer byteBuffer) {
        byte[] flightIdBytes = new byte[10];
        byteBuffer.get(flightIdBytes);
        int requestedSeats = byteBuffer.getInt();
        String flightId = new String(flightIdBytes).trim();
        Flight flight = flights.get(flightId);

        if (flight == null) {
            return String.format("No such flight: %s", flightId).getBytes();
        } else if (flight.seats < requestedSeats) {
            return String.format("Not enough seats on flight: %s", flightId).getBytes();
        } else {
            int reservationId = reservations.get(flightId).size() + 1;
            reservations.get(flightId).put(reservationId, requestedSeats);
            flight.seats -= requestedSeats;
            SeatMonitor.notifyClients(flightId, flight.seats);
            return String.format("Booking successful, Reservation ID: %d, Seats remaining: %d",
                    reservationId, flight.seats).getBytes();
        }
    }

    // case4 定義 processCancellation 方法
    private static byte[] processCancellation(ByteBuffer byteBuffer) {
        byte[] flightIdBytes = new byte[10];
        byteBuffer.get(flightIdBytes);
        String flightId = new String(flightIdBytes).trim();
        int reservationId = byteBuffer.getInt();
        Flight flight = flights.get(flightId);

        if (flight == null) {
            return String.format("No such flight: %s", flightId).getBytes();
        } else {
            Map<Integer, Integer> flightReservations = reservations.get(flightId);
            if (flightReservations == null || !flightReservations.containsKey(reservationId)) {
                return String.format("No such booking: %d", reservationId).getBytes();
            } else {
                int seatsToRelease = flightReservations.get(reservationId);
                flight.seats += seatsToRelease;
                flightReservations.remove(reservationId);
                SeatMonitor.notifyClients(flightId, flight.seats);
                return String.format("Cancellation successful, Seats remaining: %d", flight.seats).getBytes();
            }
        }
    }

    // case5 定義 processLuggage 方法
    private static byte[] processLuggage(ByteBuffer byteBuffer) {
        byte[] flightIdBytes = new byte[10];
        byteBuffer.get(flightIdBytes);
        String flightId = new String(flightIdBytes).trim();
        int reservationId = byteBuffer.getInt();
        int luggageCount = byteBuffer.getInt();
        Flight flight = flights.get(flightId);

        if (flight == null) {
            return String.format("No such flight: %s", flightId).getBytes();
        } else {
            Map<Integer, Integer> flightReservations = reservations.get(flightId);
            if (flightReservations == null || !flightReservations.containsKey(reservationId)) {
                return String.format("No such booking: %d", reservationId).getBytes();
            } else {
                int currentLuggage = flightReservations.get(reservationId);
                flightReservations.put(reservationId, currentLuggage + luggageCount);
                return String.format("Luggage added: %d, New total luggage: %d",
                        luggageCount, currentLuggage + luggageCount).getBytes();
            }
        }
    }

    // case6 定義 registerMonitor 方法
    private static byte[] registerMonitor(ByteBuffer byteBuffer, InetAddress clientAddress, int clientPort) {
        byte[] flightIdBytes = new byte[10];
        byteBuffer.get(flightIdBytes);
        String flightId = new String(flightIdBytes).trim();
        SeatMonitor.registerClient(flightId, clientAddress, clientPort);
        return String.format("Client registered for monitoring flight: %s", flightId).getBytes();
    }
    
    
    
    
    
    
    
    
    
    // 定義一個類來保存緩存的回應數據
    static class CachedResponse {
        byte[] data;
        long timestamp;

        CachedResponse(byte[] data, long timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }
    }

    // 將發送回應的邏輯封裝到一個方法中
    private static void sendResponse(DatagramSocket socket, byte[] data, InetAddress address, int port) throws IOException {
        DatagramPacket sendPacket = new DatagramPacket(data, data.length, address, port);
        socket.send(sendPacket);
    }

    //   定義 Flight
    static class Flight {
        String flightId;
        String departure;
        int price;
        int seats;
        String src;
        String dest;

        public Flight(String flightId, String departure, int price, int seats, String src, String dest) {
            this.flightId = flightId;
            this.departure = departure;
            this.price = price;
            this.seats = seats;
            this.src = src;
            this.dest = dest;
        }
    }

    static class SeatMonitor {
        private static Map<String, Map<InetAddress, Integer>> clientCallbacks = new HashMap<>();

        public static void registerClient(String flightId, InetAddress clientAddress, int clientPort) {
            clientCallbacks.computeIfAbsent(flightId, k -> new HashMap<>()).put(clientAddress, clientPort);
            System.out.println("Client monitoring for flight: " + flightId + ", Address: " + clientAddress + ", Port: " + clientPort);
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