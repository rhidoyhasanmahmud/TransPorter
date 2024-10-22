import java.io.*;
import java.net.*;

public class TcpTransport implements Transport {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;

    public TcpTransport(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.dataIn = new DataInputStream(socket.getInputStream());
        this.dataOut = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void send(String message) throws IOException {
        out.println(message);
    }

    @Override
    public String receive() throws IOException {
        return in.readLine();
    }

    @Override
    public void sendFile(byte[] data) throws IOException {
        dataOut.write(data);
        dataOut.flush();
    }

    @Override
    public byte[] receiveFile(long fileSize) throws IOException {
        byte[] data = new byte[(int) fileSize];
        dataIn.readFully(data);
        return data;
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
