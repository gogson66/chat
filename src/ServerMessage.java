import java.util.Map;

public record ServerMessage (MessageType Type, String from, long createdAt, Map<String, String> data) {
    public ServerMessage {
        data = Map.copyOf(data);
    }
}
