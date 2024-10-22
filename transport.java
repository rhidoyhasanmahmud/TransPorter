import java.io.IOException;

public interface transport extends AutoCloseable {
    void send(String message) throws IOException;
    String receive() throws IOException;
    void sendFile(byte[] data) throws IOException;
    byte[] receiveFile(long fileSize) throws IOException;
    @Override
    void close() throws IOException;
}
