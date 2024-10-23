import java.io.*;
import java.net.*;

public class TcpTransport implements Transport {
    private Socket socket;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;

    public TcpTransport(Socket socket) throws IOException {
        this.socket = socket;
        this.dataIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.dataOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    @Override
    public void send(String message) throws IOException {
        dataOut.writeUTF(message);
        dataOut.flush();
    }

    @Override
    public String receive() throws IOException {
        return dataIn.readUTF();
    }

    @Override
    public void sendFile(byte[] data) throws IOException {
        dataOut.writeInt(data.length);
        dataOut.flush();
        dataOut.write(data);
        dataOut.flush();
    }

    @Override
    public byte[] receiveFile() throws IOException {
        int length = dataIn.readInt();
        byte[] data = new byte[length];
        dataIn.readFully(data);
        return data;
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}