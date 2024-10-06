import java.net.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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
            System.out.println("Server is running...");

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                serverSocket.receive(receivePacket);
                ByteBuffer byteBuffer = ByteBuffer.wrap(receivePacket.getData());
                byte opCode = byteBuffer.get(); // 获取操作码
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                String requestID = clientAddress.toString() + clientPort + byteBuffer.toString(); // 唯一请求ID

                // 防止重复处理相同请求
                if (processedRequests.containsKey(requestID)) {
                    System.out.println("Duplicate request, ignoring.");
                    continue;
                }

                byte[] sendData;
                switch (opCode) {
                    case 1:
                        // 查询航班 (根据出发地和目的地)
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
                            sendData = matchingFlights.toString().getBytes();
                        } else {
                            sendData = String.format("No flights from %s to %s.", src, dest).getBytes();
                        }
                        break;
                        
                    case 2:
                        // 查询航班详情 (根据航班ID)
                        byte[] flightIdBytes = new byte[10];
                        byteBuffer.get(flightIdBytes);
                        String flightId = new String(flightIdBytes).trim();
                        
                        Flight flight = flights.get(flightId);
                        if (flight != null) {
                            sendData = String.format("Flight ID: %s, Departure: %s, Price: %d, Seats: %d",
                                    flight.flightId, flight.departure, flight.price, flight.seats).getBytes();
                        } else {
                            sendData = "Flight not found.".getBytes();
                        }
                        break;
                        
                    case 3:
                        // 添加预订
                        int requestedSeats = byteBuffer.getInt();
                        flightIdBytes = new byte[10];
                        byteBuffer.get(flightIdBytes);
                        flightId = new String(flightIdBytes).trim();
                        
                        flight = flights.get(flightId);
                        
                        if (flight == null) {
                            sendData = String.format("No such flight: %s", flightId).getBytes();
                        } else if (flight.seats < requestedSeats) {
                            sendData = String.format("Not enough seats on flight: %s", flightId).getBytes();
                        } else {
                            int reservationId = reservations.get(flightId).size() + 1;
                            reservations.get(flightId).put(reservationId, requestedSeats);
                            flight.seats -= requestedSeats;
                            
                            // 监控通知
                            SeatMonitor.notifyClients(flightId, flight.seats);
                            sendData = String.format("Booking successful, Reservation ID: %d, Seats remaining: %d",
                                    reservationId, flight.seats).getBytes();
                        }
                        break;

                    case 4:
                        // 取消预订
                        flightIdBytes = new byte[10];
                        byteBuffer.get(flightIdBytes);
                        flightId = new String(flightIdBytes).trim();
                        int reservationId = byteBuffer.getInt();
                        
                        flight = flights.get(flightId);
                        
                        if (flight == null) {
                            sendData = String.format("No such flight: %s", flightId).getBytes();
                        } else {
                            Map<Integer, Integer> flightReservations = reservations.get(flightId);
                            if (flightReservations == null || !flightReservations.containsKey(reservationId)) {
                                sendData = String.format("No such booking: %d", reservationId).getBytes();
                            } else {
                                int seatsToRelease = flightReservations.get(reservationId);
                                flight.seats += seatsToRelease;
                                flightReservations.remove(reservationId);
                                
                                // 监控通知
                                SeatMonitor.notifyClients(flightId, flight.seats);
                                sendData = String.format("Cancellation successful, Seats remaining: %d", flight.seats).getBytes();
                            }
                        }
                        break;

                    case 5:
                        // 添加行李
                        flightIdBytes = new byte[10];
                        byteBuffer.get(flightIdBytes);
                        flightId = new String(flightIdBytes).trim();
                        reservationId = byteBuffer.getInt();
                        int luggageCount = byteBuffer.getInt();

                        flight = flights.get(flightId);
                        if (flight == null) {
                            sendData = String.format("No such flight: %s", flightId).getBytes();
                        } else {
                            Map<Integer, Integer> flightReservations = reservations.get(flightId);
                            if (flightReservations == null || !flightReservations.containsKey(reservationId)) {
                                sendData = String.format("No such booking: %d", reservationId).getBytes();
                            } else {
                                // 增加行李数量
                                int currentLuggage = flightReservations.get(reservationId);
                                flightReservations.put(reservationId, currentLuggage + luggageCount);
                                sendData = String.format("Luggage added: %d, New total luggage: %d",
                                        luggageCount, currentLuggage + luggageCount).getBytes();
                            }
                        }
                        break;

                    case 6:
                        // 注册航班座位监控
                        flightIdBytes = new byte[10];
                        byteBuffer.get(flightIdBytes);
                        flightId = new String(flightIdBytes).trim();

                        SeatMonitor.registerClient(flightId, clientAddress, clientPort);
                        sendData = String.format("Client registered for monitoring flight: %s", flightId).getBytes();
                        break;

                    default:
                        sendData = "Unknown operation.".getBytes();
                }

                // 模拟消息丢失
                Random random = new Random();
                if (random.nextInt(10) > 2) {
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                    serverSocket.send(sendPacket);
                    System.out.println("Response sent.");
                    processedRequests.put(requestID, true);  // 请求已处理
                } else {
                    System.out.println("Simulated message loss, response not sent.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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