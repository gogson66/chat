import java.util.Map;

public record ClientMessage (MessageType Type, String from, Map<String, String> data) {
    public ClientMessage {
        data = Map.copyOf(data);
    }
}
