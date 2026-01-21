package fr.ynryo.ouestcefdpdetram;

public class MarkerData {
    private String id;
    private String lineNumber;
    private Position position;
    private String fillColor;
    private String color;

    public MarkerData(String id, String lineNumber, Position position, String fillColor, String color) {
        this.id = id;
        this.lineNumber = lineNumber;
        this.position = position;
        this.fillColor = fillColor;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public String getLineNumber() {
        return lineNumber;
    }

    public Position getPosition() {
        return position;
    }

    public String getFillColor() {
        return fillColor;
    }
    
    public String getColor() {
        return color;
    }
}
