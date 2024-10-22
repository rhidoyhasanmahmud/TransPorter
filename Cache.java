import java.util.concurrent.ConcurrentHashMap;

public class Cache {
    private ConcurrentHashMap<String, byte[]> cache;

    public Cache() {
        cache = new ConcurrentHashMap<>();
    }

    public void add(String filename, byte[] data) {
        cache.put(filename, data);
    }

    public byte[] get(String filename) {
        return cache.get(filename);
    }

    public boolean exists(String filename) {
        return cache.containsKey(filename);
    }
}
