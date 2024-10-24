import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final CacheManager cacheManager;

    public ClientHandler(Socket socket, CacheManager cacheManager) {
        this.socket = socket;
        this.cacheManager = cacheManager;
    }

    @Override
    public void run() {
        try (
                DataInputStream dataIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        ) {
            String command = dataIn.readUTF();
            if (command.startsWith("GET ")) {
                handleGet(command.substring(4).trim(), dataOut);
            } else if (command.startsWith("STORE ")) {
                handleStore(command.substring(6).trim(), dataIn, dataOut);
            } else {
                dataOut.writeUTF("INVALID_COMMAND");
                dataOut.flush();
                System.err.println("Received invalid command.");
            }
        } catch (IOException e) {
            System.err.println("Error handling client request.");
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private void handleGet(String filename, DataOutputStream dataOut) throws IOException {
        if (cacheManager.contains(filename)) {
            dataOut.writeUTF("FOUND");
            byte[] data = cacheManager.getFile(filename);
            dataOut.writeInt(data.length);
            dataOut.write(data);
            dataOut.flush();
        } else {
            dataOut.writeUTF("NOT_FOUND");
            dataOut.flush();
        }
    }

    private void handleStore(String filename, DataInputStream dataIn, DataOutputStream dataOut) throws IOException {
        int size = dataIn.readInt();
        byte[] data = new byte[size];
        dataIn.readFully(data);
        cacheManager.storeFile(filename, data);
        dataOut.writeUTF("STORED");
        dataOut.flush();
    }
}
