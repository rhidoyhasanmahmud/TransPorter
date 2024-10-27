import java.io.Closeable;
import java.io.IOException;

public interface DataTransport extends Closeable {
    void transmitMessage(String message) throws IOException;
    String receiveMessage() throws IOException;
    void transmitFile(byte[] fileData) throws IOException;
    byte[] receiveFileData() throws IOException;
}
