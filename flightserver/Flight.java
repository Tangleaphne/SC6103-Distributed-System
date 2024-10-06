package flightserver;
// Flight.java
public class Flight {
    String flightId;
    String departure;
    int price;
    int seats;
    String src;  // 出发地
    String dest; // 目的地

    // 构造函数，确保所有字段都被初始化
    public Flight(String flightId, String departure, int price, int seats, String src, String dest) {
        this.flightId = flightId;
        this.departure = departure;
        this.price = price;
        this.seats = seats;
        this.src = src;
        this.dest = dest;
    }

    // 添加 getter 方法来访问字段
    public String getFlightId() {
        return flightId;
    }

    public String getDeparture() {
        return departure;
    }

    public int getPrice() {
        return price;
    }

    public int getSeats() {
        return seats;
    }

    public void setSeats(int seats) {
        this.seats = seats;
    }

    public String getSrc() {
        return src;
    }

    public String getDest() {
        return dest;
    }

    @Override
    public String toString() {
        return "\nFlight " + flightId + ": " + src + " -> " + dest + ", Departure: " + departure + ", Price: " + price + ", Seats: " + seats;
    }
}
