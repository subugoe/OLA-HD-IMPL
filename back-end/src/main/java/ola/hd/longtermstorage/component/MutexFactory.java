package ola.hd.longtermstorage.component;

import org.springframework.stereotype.Component;
import org.springframework.util.ConcurrentReferenceHashMap;

@Component
public class MutexFactory<K> {

    private ConcurrentReferenceHashMap<K, Object> map;

    public MutexFactory() {
        this.map = new ConcurrentReferenceHashMap<>();
    }

    public Object getMutex(K key) {
        return this.map.compute(key, (k, v) -> v == null ? new Object() : v);
    }
}
