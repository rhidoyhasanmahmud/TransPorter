import java.io.*;
import java.net.Socket;

public class snw_transport implements Transport {
    private final Socket socket;
    private final DataInputStream dataIn;
    private final DataOutputStream dataOut;
    private static final int CHUNK_SIZE = 1024;

    public snw_transport(Socket socket) throws IOException {
        this.socket = socket;
        this.dataIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.dataOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    @Override
    public void send(String message) throws IOException {
        dataOut.writeUTF(message);
        dataOut.flush();
        String ack = dataIn.readUTF();
        if (!"ACK".equals(ack)) {
            throw new IOException("ACK not received");
        }
    }

    @Override
    public String receive() throws IOException {
        String message = dataIn.readUTF();
        dataOut.writeUTF("ACK");
        dataOut.flush();
        return message;
    }

    @Override
    public void sendFile(byte[] data) throws IOException {
        dataOut.writeInt(data.length);
        dataOut.flush();
        String ack = dataIn.readUTF();
        if (!"ACK".equals(ack)) {
            throw new IOException("ACK not received after sending file length");
        }
        long totalSent = 0;
        while (totalSent < data.length) {
            int bytesToSend = (int) Math.min(CHUNK_SIZE, data.length - totalSent);
            dataOut.write(data, (int) totalSent, bytesToSend);
            dataOut.flush();
            totalSent += bytesToSend;
            ack = dataIn.readUTF();
            if (!"ACK".equals(ack)) {
                throw new IOException("ACK not received during file transfer");
            }
        }
    }

    @Override
    public byte[] receiveFile() throws IOException {
        int fileSize = dataIn.readInt();
        dataOut.writeUTF("ACK");
        dataOut.flush();

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
            dataOut.writeUTF("ACK");
            dataOut.flush();
        }
        return baos.toByteArray();
    }

    @Override
    public void close() throws IOException {
        dataIn.close();
        dataOut.close();
        socket.close();
    }
}
