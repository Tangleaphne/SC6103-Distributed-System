package flightserver;

import java.net.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.io.IOException;

import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Server {
    private static final int SERVER_PORT = 12345;

    // 存储航班信息 Store flight information
    private static Map<String, Flight> flights = new HashMap<>();
    // 存储每个航班的预订信息 Store booking information for each flight
    private static Map<String, Map<Integer, Integer>> reservations = new HashMap<>();
    // 存储已处理的请求以实现 at-most-once 语义
    // private static Map<String, Boolean> processedRequests = new HashMap<>();

    // Store processed requests to ensure at-most-once semantics
    private static Map<String, CachedResponse> processedRequests = new HashMap<>();

    // Client callbacks for seat notifications

    static {
        // 初始化航班信息
        flights.put("FL123", new Flight("FL123", "09:00", 300, 100, "Singapore", "Penang"));
        flights.put("FL456", new Flight("FL456", "12:00", 450, 50, "Shanghai", "Taipei"));
        flights.put("FL789", new Flight("FL789", "08:15", 260, 180, "Tokyo", "Beijing"));
        flights.put("FL910", new Flight("FL910", "20:15", 360, 200, "Tokyo", "Beijing"));
        // 初始化每个航班的预订信息存储
        reservations.put("FL123", new HashMap<>());
        reservations.put("FL456", new HashMap<>());
        reservations.put("FL789", new HashMap<>());
        reservations.put("FL910", new HashMap<>());
    }

    public static void main(String[] args) {

        // try (DatagramSocket serverSocket = new DatagramSocket(SERVER_PORT,
        // InetAddress.getByName("10.91.61.102"))) {
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
                // String requestID = clientAddress.toString() + clientPort +
                // byteBuffer.toString(); // 唯一请求ID

                // Generate unique UUID for each request
                String requestID = UUID.randomUUID().toString();
                System.out.println("Generated Request ID: " + requestID);

                // Process client request
                String clientMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                handleClientRequest(clientMessage, requestID, clientAddress, clientPort);

                /*
                 * 防止重复处理相同请求
                 * if (processedRequests.containsKey(requestID)) {
                 * System.out.println("Duplicate request, ignoring.");
                 * continue;
                 * }
                 */

                // 檢查是否有最近的回應 (3 秒內)
                long currentTime = System.currentTimeMillis();
                if (processedRequests.containsKey(requestID)) {
                    CachedResponse cachedResponse = processedRequests.get(requestID);
                    if (currentTime - cachedResponse.timestamp < 3000) {
                        // 在 3 秒內，使用上次的回應結果
                        System.out.println("Returning cached response for request: " + requestID + "\n");
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

                    case 7:
                        sendData = getServerTime(clientAddress, clientPort);
                        System.out.println("Server time: " + new String(sendData)); // Print the server time
                        break;

                    default:
                        sendData = "Unknown operation.".getBytes();
                }

                // 更新緩存回應
                processedRequests.put(requestID, new CachedResponse(sendData, currentTime));

                // 模拟消息丢失
                Random random = new Random();
                if (random.nextInt(10) > 1) {
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress,
                            clientPort);
                    serverSocket.send(sendPacket);
                    System.out.println("Response sent.");
                    processedRequests.put(requestID, new CachedResponse(sendData, currentTime)); // 请求已处理
                } else {
                    System.out.println("Simulated message loss, response not sent.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // %%%
    // This method handles the client requests while keeping the UUID tracking
    // feature
    private static void handleClientRequest(String clientMessage, String requestID, InetAddress clientAddress,
            int clientPort) {
        // Check if the request has already been processed
        if (processedRequests.containsKey(requestID)) {
            System.out.println("Duplicate request, ignoring.");
            return;
        }
        // Log the request ID for tracking
        System.out.println("Received request with ID: " + requestID + " from client: " + clientAddress);

        // Process the request (existing logic, for flight queries, seat availability,
        // etc.)
        processFlightQuery(clientMessage);

        // Store the processed request with its ID to ensure at-most-once semantics
        processedRequests.put(requestID, new CachedResponse(clientMessage.getBytes(),
                System.currentTimeMillis()));
    }

    // %%%

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
                matchingFlights.append(String.format("Flight ID: %s, Departure: %s, Price: %d, Seats: %d \n",
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
            return FlightMarshaller.marshall(flight);
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
            System.out.println("seat passed: " + flight.seats);
            flight.seats -= requestedSeats;
            SeatMonitor.notifyClients(flightId, flight.seats);
            return String.format("Booking successful, Reservation ID: %d, Seats booked: %d, Seats remaining: %d",
                    reservationId, requestedSeats, flight.seats).getBytes();
        }
    }

    // case4 定義 processCancellation 方法
    private static byte[] processCancellation(ByteBuffer byteBuffer) {
        byte[] flightIdBytes = new byte[10];
        byteBuffer.get(flightIdBytes);
        String flightId = new String(flightIdBytes).trim();
        int reservationId = byteBuffer.getInt();
        Flight flight = flights.get(flightId);

        System.out.println("Checking reservation for ID: " + reservationId + " in flight: " + flightId);

        if (flight == null) {
            return String.format("No such flight: %s", flightId).getBytes();
        } else {
            Map<Integer, Integer> flightReservations = reservations.get(flightId);
            if (flightReservations == null || !flightReservations.containsKey(reservationId)) {
                return String.format("No such booking: %d", reservationId).getBytes();
            } else {
                System.out.println("Found booking with Reservation ID: " + reservationId + ", Seats to release: "
                        + flightReservations.get(reservationId));
                int seatsToRelease = flightReservations.get(reservationId);
                flight.seats += seatsToRelease;
                // flightReservations.remove(reservationId);
                SeatMonitor.notifyClients(flightId, flight.seats);
                return String.format("Reservation ID %d on flight %s cancellation successful, Seats remaining: %d",
                        reservationId, flightId, flight.seats).getBytes();
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

    // case7 定義 getServerTime 方法
    private static byte[] getServerTime(InetAddress clientAddress, int clientPort) {
        // Return the current server time in yyyy-MM-dd HH:mm:ss format
        ZoneId sgtZoneId = ZoneId.of("Asia/Singapore");
        ZonedDateTime currentTime = ZonedDateTime.now(sgtZoneId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedTime = currentTime.format(formatter);
        System.out.println("Current server time: " + formattedTime); // Print the server time
        return formattedTime.getBytes();
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
    private static void sendResponse(DatagramSocket socket, byte[] data, InetAddress address, int port)
            throws IOException {
        DatagramPacket sendPacket = new DatagramPacket(data, data.length, address, port);
        socket.send(sendPacket);
    }

    // Marshalling
    public static class FlightMarshaller {
        public static byte[] marshall(Flight flight) {
            byte[] flightIdBytes = stringToBytes(flight.getFlightId(), 20);
            byte[] departureBytes = stringToBytes(flight.getDeparture(), 20);
            byte[] srcBytes = stringToBytes(flight.getSrc(), 20); // 添加 src 字段的序列化
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

    static class SeatMonitor {
        private static Map<String, Map<InetAddress, Integer>> clientCallbacks = new HashMap<>();

        public static void registerClient(String flightId, InetAddress clientAddress, int clientPort) {
            clientCallbacks.computeIfAbsent(flightId, k -> new HashMap<>()).put(clientAddress, clientPort);
            System.out.println("Client monitoring for flight: " + flightId + ", Address: " + clientAddress + ", Port: "
                    + clientPort);
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
                System.out.println("Notifying clients about updated seats for flight: " + flightId
                        + ", Seats remaining: " + seatsAvailable);
                for (Map.Entry<InetAddress, Integer> entry : clients.entrySet()) {
                    System.out.println("Notifying client: " + entry.getKey() + ":" + entry.getValue());
                    try (DatagramSocket socket = new DatagramSocket()) {
                        String message = String.format("Updated seat availability for flight %s: %d seats remaining",
                                flightId, seatsAvailable);
                        byte[] sendData = message.getBytes();
                        DatagramPacket packet = new DatagramPacket(sendData, sendData.length, entry.getKey(),
                                entry.getValue());
                        socket.send(packet);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        }

    }

    // Existing logic to process flight queries (no modification needed here)
    private static void processFlightQuery(String message) {
        // Existing logic to process the flight query
        System.out.println(
                "Processing flight query: " + new String(message.getBytes(), StandardCharsets.UTF_8).trim() + "\n");

    }

}
