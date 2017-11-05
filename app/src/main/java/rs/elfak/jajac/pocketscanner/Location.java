package rs.elfak.jajac.pocketscanner;

public class Location {

    private double lat;
    private double lon;

    public Location() {

    }

    public Location(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public double getLat() {
        return this.lat;
    }

    public double getLon() {
        return this.lon;
    }

}
