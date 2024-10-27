import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class cache {
    private final int cachePort;
    private final String connectionProtocol;
    private final String primaryServerIp;
    private final int primaryServerPort;
    private final CacheStorageManager cacheStorage;
    private final ExecutorService clientExecutor;

    public cache(int cachePort, String connectionProtocol, String primaryServerIp, int primaryServerPort) throws IOException {
        this.cachePort = cachePort;
        this.connectionProtocol = connectionProtocol;
        this.primaryServerIp = primaryServerIp;
        this.primaryServerPort = primaryServerPort;
        this.cacheStorage = new CacheStorageManager("cache_storage");
        this.clientExecutor = Executors.newCachedThreadPool();
    }

    public static void main(String[] args) {
        try {
            int port = 5050;
            String cacheIp = "localhost";
            String protocol = "tcp";
            String serverIp = "localhost";
            int serverPort = 4040;

            if (args.length >= 1) {
                port = Integer.parseInt(args[0]);
            }
            if (args.length >= 2) {
                cacheIp = args[1];
            }
            if (args.length >= 3) {
                protocol = args[2];
            }
            if (args.length >= 4) {
                serverIp = args[3];
            }
            if (args.length >= 5) {
                serverPort = Integer.parseInt(args[4]);
            }
            cache cacheService = new cache(port, protocol, serverIp, serverPort);
            cacheService.initializeCache();
        } catch (IOException e) {
            System.err.println("IO Exception occurred while starting the cache service.");
        } catch (NumberFormatException e) {
            System.out.println("Usage: java CacheService [port] [tcp/snw] [server ip] [server port]");
        }
    }

    public void initializeCache() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(cachePort)) {
            System.out.println("Cache service started on port " + cachePort + " using protocol: " + connectionProtocol.toUpperCase());

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientExecutor.execute(new CacheClientHandler(clientSocket, cacheStorage));
                } catch (IOException e) {
                    System.err.println("Error accepting client connection.");
                }
            }
        }
    }
}
