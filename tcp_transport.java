import java.io.*;
import java.net.Socket;

public class tcp_transport implements DataTransport {
    private final Socket socket;
    private final DataInputStream dataIn;
    private final DataOutputStream dataOut;

    public tcp_transport(Socket socket) throws IOException {
        this.socket = socket;
        this.dataIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.dataOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    @Override
    public void transmitMessage(String message) throws IOException {
        dataOut.writeUTF(message);
        dataOut.flush();
    }

    @Override
    public String receiveMessage() throws IOException {
        return dataIn.readUTF();
    }

    @Override
    public void transmitFile(byte[] data) throws IOException {
        dataOut.writeInt(data.length);
        dataOut.write(data);
        dataOut.flush();
    }

    @Override
    public byte[] receiveFileData() throws IOException {
        int length = dataIn.readInt();
        byte[] data = new byte[length];
        dataIn.readFully(data);
        return data;
    }

    @Override
    public void close() throws IOException {
        dataIn.close();
        dataOut.close();
        socket.close();
    }
}
