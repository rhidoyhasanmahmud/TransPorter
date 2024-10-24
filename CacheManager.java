import java.io.IOException;
import java.nio.file.*;

public class CacheManager {
    private final Path cacheDirectory;

    public CacheManager(String cacheDir) throws IOException {
        this.cacheDirectory = Paths.get(cacheDir);
        Files.createDirectories(cacheDirectory);
    }

    public boolean contains(String filename) {
        return Files.exists(cacheDirectory.resolve(filename));
    }

    public byte[] getFile(String filename) throws IOException {
        return Files.readAllBytes(cacheDirectory.resolve(filename));
    }

    public void storeFile(String filename, byte[] data) throws IOException {
        Files.write(cacheDirectory.resolve(filename), data);
    }
}
