import java.io.*;
import java.net.Socket;

public class CacheClientHandler implements Runnable {
    private final Socket clientSocket;
    private final CacheStorageManager cacheStorage;

    public CacheClientHandler(Socket clientSocket, CacheStorageManager cacheStorage) {
        this.clientSocket = clientSocket;
        this.cacheStorage = cacheStorage;
    }

    @Override
    public void run() {
        try (
                DataInputStream inputStream = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
                DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
        ) {
            String clientCommand = inputStream.readUTF();
            if (clientCommand.startsWith("GET ")) {
                executeGet(clientCommand.substring(4).trim(), outputStream);
            } else if (clientCommand.startsWith("STORE ")) {
                executeStore(clientCommand.substring(6).trim(), inputStream, outputStream);
            } else {
                outputStream.writeUTF("INVALID_COMMAND");
                outputStream.flush();
                System.err.println("Received an invalid command.");
            }
        } catch (IOException e) {
            System.err.println("Error processing client request.");
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
        }
    }

    private void executeGet(String fileName, DataOutputStream outputStream) throws IOException {
        if (cacheStorage.isFileCached(fileName)) {
            outputStream.writeUTF("FOUND");
            byte[] fileData = cacheStorage.retrieveFile(fileName);
            outputStream.writeInt(fileData.length);
            outputStream.write(fileData);
            outputStream.flush();
        } else {
            outputStream.writeUTF("NOT_FOUND");
            outputStream.flush();
        }
    }

    private void executeStore(String fileName, DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
        int fileSize = inputStream.readInt();
        byte[] fileData = new byte[fileSize];
        inputStream.readFully(fileData);
        cacheStorage.saveFile(fileName, fileData);
        outputStream.writeUTF("STORED");
        outputStream.flush();
    }
}
