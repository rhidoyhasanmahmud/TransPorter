import java.io.*;
import java.net.*;

public class snw_transport implements transport {
    private Socket socket;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    private static final int CHUNK_SIZE = 1024; // For Stop-and-Wait protocol

    public snw_transport(Socket socket) throws IOException {
        this.socket = socket;
        this.dataIn = new DataInputStream(socket.getInputStream());
        this.dataOut = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void send(String message) throws IOException {
        dataOut.writeUTF(message);
        dataOut.flush();
        // Wait for ACK
        String ack = dataIn.readUTF();
        if (!"ACK".equals(ack)) {
            throw new IOException("ACK not received");
        }
    }

    @Override
    public String receive() throws IOException {
        String message = dataIn.readUTF();
        // Send ACK
        dataOut.writeUTF("ACK");
        dataOut.flush();
        return message;
    }

    @Override
    public void sendFile(byte[] data) throws IOException {
        dataOut.writeLong(data.length);
        long totalSent = 0;
        while (totalSent < data.length) {
            int bytesToSend = (int) Math.min(CHUNK_SIZE, data.length - totalSent);
            dataOut.write(data, (int) totalSent, bytesToSend);
            dataOut.flush();
            totalSent += bytesToSend;
            // Wait for ACK
            String ack = dataIn.readUTF();
            if (!"ACK".equals(ack)) {
                throw new IOException("ACK not received during file transfer");
            }
        }
    }

    @Override
    public byte[] receiveFile(long fileSize) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        long totalReceived = 0;
        byte[] buffer = new byte[CHUNK_SIZE];
        while (totalReceived < fileSize) {
            int bytesToRead = (int) Math.min(CHUNK_SIZE, fileSize - totalReceived);
            int bytesRead = dataIn.read(buffer, 0, bytesToRead);
            if (bytesRead == -1) {
                throw new IOException("Unexpected end of stream during file transfer");
            }
            baos.write(buffer, 0, bytesRead);
            totalReceived += bytesRead;
            // Send ACK
            dataOut.writeUTF("ACK");
            dataOut.flush();
        }
        return baos.toByteArray();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
