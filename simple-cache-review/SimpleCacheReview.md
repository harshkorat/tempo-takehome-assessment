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

## Overview
The implementation is simple and readable, but it is not production-ready for a highly concurrent application with:
- thousands of reads per second
- hundreds of writes per second
- tens of concurrent threads

The main issues are correctness, memory growth, stale entry handling, and time-related reliability.

---

## 1. Expired entries are never removed

### Issue
When an entry expires, `get()` returns `null`, but the expired entry remains in the `ConcurrentHashMap`.

```kotlin
if (System.currentTimeMillis() - entry.timestamp < ttlMs) {
    return entry.value
}
return null