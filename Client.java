import java.io.*;
import java.net.*;
import java.nio.file.*;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 12345;
    private Transport transport;

    public Client(Transport transport) {
        this.transport = transport;
    }

    public void start() throws IOException {
        BufferedReader console = new BufferedReader(
                new InputStreamReader(System.in));
        String command;
        System.out.println("Enter commands (put filename / get filename):");
        while ((command = console.readLine()) != null) {
            if (command.startsWith("put ")) {
                handlePut(command.substring(4).trim());
            } else if (command.startsWith("get ")) {
                handleGet(command.substring(4).trim());
            } else {
                transport.send(command);
                System.out.println(transport.receive());
            }
            System.out.println("Enter next command:");
        }
        transport.close();
    }

    private void handlePut(String filename) throws IOException {
        Path filePath = Paths.get("client_files/" + filename);
        if (!Files.exists(filePath)) {
            System.out.println("File '" + filename +
                    "' does not exist in client_files directory.");
            return;
        }
        transport.send("put " + filename);
        String response = transport.receive();
        if ("READY".equals(response)) {
            byte[] data = Files.readAllBytes(filePath);
            // Send file size to server
            transport.send(String.valueOf(data.length));
            // Wait for server acknowledgment
            response = transport.receive();
            if (!"SIZE_RECEIVED".equals(response)) {
                System.out.println("Server did not acknowledge file size.");
                return;
            }
            // Send file data
            transport.sendFile(data);
            // Wait for server confirmation
            String serverResponse = transport.receive();
            if ("UPLOAD_SUCCESS".equals(serverResponse)) {
                // Delete the file from client_files
                Files.delete(filePath);
                System.out.println("File '" + filename + "' uploaded and moved to server successfully.");
            } else {
                System.out.println("Server response: " + serverResponse);
            }
        } else {
            System.out.println("Server response: " + response);
        }
    }

    private void handleGet(String filename) throws IOException {
        transport.send("get " + filename);
        String response = transport.receive();
        if ("READY".equals(response)) {
            // Receive file size
            String sizeStr = transport.receive();
            long fileSize = Long.parseLong(sizeStr);
            // Send acknowledgment
            transport.send("SIZE_RECEIVED");
            byte[] data = transport.receiveFile(fileSize);
            // Save to client_files
            Files.write(Paths.get("client_files/" + filename), data);
            System.out.println("File '" + filename + "' downloaded successfully.");
        } else if (response.startsWith("ERROR:")) {
            System.out.println(response);
        } else {
            System.out.println("Unexpected server response: " + response);
        }
    }

    public static void main(String[] args) {
        try {
            Socket socket = new Socket(SERVER_ADDRESS, PORT);
            // Choose transport implementation
            Transport transport = new TcpTransport(socket);
            // Or use Stop-and-Wait:
            // Transport transport = new SnwTransport(socket);
            Client client = new Client(transport);
            client.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
