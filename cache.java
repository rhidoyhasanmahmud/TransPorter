import java.util.concurrent.ConcurrentHashMap;

public class cache {
    private ConcurrentHashMap<String, byte[]> cache;

    public cache() {
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
