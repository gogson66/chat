import java.util.Map;

public record Message (MessageType Type, String from, long timestamp, Map<String, String> data) {
    public Message {
        data = Map.copyOf(data);
    }
}
