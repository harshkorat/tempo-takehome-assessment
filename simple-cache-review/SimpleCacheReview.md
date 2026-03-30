## Code Review

You are reviewing the following code submitted as part of a task to implement an item cache in a highly concurrent application. The anticipated load includes: thousands of reads per second, hundreds of writes per second, tens of concurrent threads.
Your objective is to identify and explain the issues in the implementation that must be addressed before deploying the code to production. Please provide a clear explanation of each issue and its potential impact on production behaviour.

```kotlin
import java.util.concurrent.ConcurrentHashMap

class SimpleCache<K, V> {
    private val cache = ConcurrentHashMap<K, CacheEntry<V>>()
    private val ttlMs = 60000 // 1 minute
    
    data class CacheEntry<V>(val value: V, val timestamp: Long)
    
    fun put(key: K, value: V) {
        cache[key] = CacheEntry(value, System.currentTimeMillis())
    }
    
    fun get(key: K): V? {
        val entry = cache[key]
        if (entry != null) {
            if (System.currentTimeMillis() - entry.timestamp < ttlMs) {
                return entry.value
            }
        }
        return null
    }
    
    fun size(): Int {
        return cache.size
    }
}
```
# SimpleCache Code Review

The implementation is straightforward and easy to understand, but there are a few issues that would need to be addressed before using it in production, especially for a system with high concurrency and sustained traffic.

## Issues Identified

### 1. Expired entries are never removed
If an item has expired, `get()` correctly returns `null`, but the expired entry still remains in the map.

**Why this matters:**  
Over time, the cache will keep holding stale data that is no longer usable. In a long-running service, this can lead to unnecessary memory growth and inaccurate cache size reporting.

---

### 2. `size()` does not reflect usable entries
The `size()` method returns the total number of entries in the map, including expired ones.

**Why this matters:**  
This makes the cache size misleading and can cause confusion during monitoring or debugging, since the reported size is not the same as the number of valid cached items.

---

### 3. Expiration logic depends on `System.currentTimeMillis()`
TTL is calculated using wall-clock time.

**Why this matters:**  
If the system clock changes for any reason (for example NTP sync or clock drift), expiration behaviour may become unreliable. Entries could expire earlier or later than expected.

A monotonic time source such as `System.nanoTime()` would be safer for measuring elapsed time.

---

### 4. Expired items continue to be checked on every read
Once an entry has expired, every future read still loads it from the map and checks the timestamp again.

**Why this matters:**  
This creates unnecessary overhead, especially in a read-heavy system. It is more efficient to remove expired entries once they are detected.

---

### 5. No eviction or capacity control
The cache has a TTL, but there is no maximum size or eviction policy.

**Why this matters:**  
If a large number of unique keys are written, the cache can keep growing and put pressure on memory even if entries are short-lived.

---

### 6. TTL is hardcoded
The TTL is fixed at 60 seconds.

**Why this matters:**  
This makes the cache less flexible and harder to tune for different environments or use cases.

---

## Final Thoughts

This is a good **basic implementation**, but I would not consider it production-ready in its current form for a highly concurrent application.

The most important fixes would be:

- removing expired entries
- avoiding misleading cache size
- using a safer time source for TTL
- adding some form of eviction / size control

If this were intended for production use, I would likely recommend using a well-tested caching library such as **Caffeine** rather than maintaining this behaviour manually.