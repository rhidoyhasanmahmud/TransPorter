import java.io.Closeable;
import java.io.IOException;

public interface Transport extends Closeable {
    void send(String message) throws IOException;
    String receive() throws IOException;
    void sendFile(byte[] data) throws IOException;
    byte[] receiveFile() throws IOException;
}
