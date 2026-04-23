package logger.enums;
public enum Severity {
    HIGH("high"),
    WARN("warn"),
    LOW("low");
    private final String name;
    Severity(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
}
