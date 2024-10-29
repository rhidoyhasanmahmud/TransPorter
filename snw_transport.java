import java.io.*;
import java.net.*;
import java.util.Arrays;

public class snw_transport implements DataTransport {
    private DatagramSocket socket;
    private InetAddress remoteAddress;
    private int remotePort;
    private static final int CHUNK_SIZE = 1024;
    private static final int TIMEOUT = 5000;
    private static final int MAX_RETRIES = 5;
    private int sequenceNumber = 0;

    public snw_transport(InetAddress remoteAddress, int remotePort, int localPort) throws SocketException {
        this.socket = new DatagramSocket(localPort);
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.socket.setSoTimeout(TIMEOUT);
    }

    public snw_transport(int localPort) throws SocketException {
        this.socket = new DatagramSocket(localPort);
        this.socket.setSoTimeout(TIMEOUT);
    }

    private byte[] intToBytes(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value
        };
    }

    private int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
                ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8) |
                (bytes[3] & 0xFF);
    }

    @Override
    public void transmitMessage(String message) throws IOException {
        byte[] payload = message.getBytes("UTF-8");
        int retries = 0;
        boolean ackReceived = false;

        while (!ackReceived && retries < MAX_RETRIES) {
            byte[] packetData = new byte[1 + 4 + payload.length];
            packetData[0] = 0; // Data packet
            System.arraycopy(intToBytes(sequenceNumber), 0, packetData, 1, 4);
            System.arraycopy(payload, 0, packetData, 5, payload.length);
            DatagramPacket packet = new DatagramPacket(packetData, packetData.length, remoteAddress, remotePort);
            socket.send(packet);

            try {
                byte[] ackBuffer = new byte[5];
                DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
                socket.receive(ackPacket);

                if (ackPacket.getLength() >= 5 && ackBuffer[0] == 1) {
                    int ackSeqNum = bytesToInt(Arrays.copyOfRange(ackBuffer, 1, 5));
                    if (ackSeqNum == sequenceNumber) {
                        ackReceived = true;
                        sequenceNumber = (sequenceNumber + 1) % Integer.MAX_VALUE;
                    }
                }
            } catch (SocketTimeoutException e) {
                retries++;
            }
        }

        if (!ackReceived) {
            throw new IOException("Failed to receive ACK after " + MAX_RETRIES + " attempts");
        }
    }

    @Override
    public String receiveMessage() throws IOException {
        while (true) {
            byte[] buffer = new byte[CHUNK_SIZE + 5];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
            } catch (SocketTimeoutException e) {
                continue;
            }

            if (packet.getLength() >= 5 && buffer[0] == 0) {
                int seqNum = bytesToInt(Arrays.copyOfRange(buffer, 1, 5));
                byte[] payload = Arrays.copyOfRange(buffer, 5, packet.getLength());
                String message = new String(payload, "UTF-8");

                byte[] ackData = new byte[5];
                ackData[0] = 1; // ACK packet
                System.arraycopy(intToBytes(seqNum), 0, ackData, 1, 4);
                DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, packet.getAddress(), packet.getPort());
                socket.send(ackPacket);

                if (remoteAddress == null) {
                    remoteAddress = packet.getAddress();
                    remotePort = packet.getPort();
                }

                return message;
            }
        }
    }


    @Override
    public void transmitFile(byte[] fileData) throws IOException {
        String fileSizeMessage = String.valueOf(fileData.length);
        transmitMessage(fileSizeMessage);

        int totalChunks = (fileData.length + CHUNK_SIZE - 1) / CHUNK_SIZE;
        int retries;
        boolean ackReceived;

        for (int i = 0; i < totalChunks; i++) {
            int offset = i * CHUNK_SIZE;
            int length = Math.min(CHUNK_SIZE, fileData.length - offset);
            byte[] chunk = Arrays.copyOfRange(fileData, offset, offset + length);
            retries = 0;
            ackReceived = false;

            while (!ackReceived && retries < MAX_RETRIES) {
                byte[] packetData = new byte[1 + 4 + chunk.length];
                packetData[0] = 0; // Data packet
                System.arraycopy(intToBytes(sequenceNumber), 0, packetData, 1, 4);
                System.arraycopy(chunk, 0, packetData, 5, chunk.length);
                DatagramPacket packet = new DatagramPacket(packetData, packetData.length, remoteAddress, remotePort);
                socket.send(packet);

                try {
                    byte[] ackBuffer = new byte[5];
                    DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
                    socket.receive(ackPacket);

                    if (ackPacket.getLength() >= 5 && ackBuffer[0] == 1) {
                        int ackSeqNum = bytesToInt(Arrays.copyOfRange(ackBuffer, 1, 5));
                        if (ackSeqNum == sequenceNumber) {
                            ackReceived = true;
                            sequenceNumber = (sequenceNumber + 1) % Integer.MAX_VALUE;
                        }
                    }
                } catch (SocketTimeoutException e) {
                    retries++;
                }
            }

            if (!ackReceived) {
                throw new IOException("Failed to receive ACK after " + MAX_RETRIES + " attempts");
            }
        }
    }

    @Override
    public byte[] receiveFileData() throws IOException {
        // First, receive the file size
        String fileSizeMessage = receiveMessage();
        int fileSize = Integer.parseInt(fileSizeMessage);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int totalReceived = 0;

        while (totalReceived < fileSize) {
            byte[] buffer = new byte[CHUNK_SIZE + 5];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            if (packet.getLength() >= 5 && buffer[0] == 0) {
                int seqNum = bytesToInt(Arrays.copyOfRange(buffer, 1, 5));
                byte[] chunk = Arrays.copyOfRange(buffer, 5, packet.getLength());
                baos.write(chunk);
                totalReceived += chunk.length;

                // Send ACK
                byte[] ackData = new byte[5];
                ackData[0] = 1; // ACK packet
                System.arraycopy(intToBytes(seqNum), 0, ackData, 1, 4);
                DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, packet.getAddress(), packet.getPort());
                socket.send(ackPacket);

                // Set remote address and port if not already set (server-side)
                if (remoteAddress == null) {
                    remoteAddress = packet.getAddress();
                    remotePort = packet.getPort();
                }
            }
        }

        return baos.toByteArray();
    }

    @Override
    public void close() {
        socket.close();
    }
}
