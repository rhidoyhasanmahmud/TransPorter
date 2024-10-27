import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CacheStorageManager {
    private final Path storageDirectory;

    public CacheStorageManager(String storageDir) throws IOException {
        this.storageDirectory = Paths.get(storageDir);
        Files.createDirectories(storageDirectory);
    }

    public boolean isFileCached(String fileName) {
        return Files.exists(storageDirectory.resolve(fileName));
    }

    public byte[] retrieveFile(String fileName) throws IOException {
        return Files.readAllBytes(storageDirectory.resolve(fileName));
    }

    public void saveFile(String fileName, byte[] fileData) throws IOException {
        Files.write(storageDirectory.resolve(fileName), fileData);
    }
}
