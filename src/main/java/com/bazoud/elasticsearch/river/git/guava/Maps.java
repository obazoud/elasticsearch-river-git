package com.bazoud.elasticsearch.river.git.guava;

import java.util.Iterator;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;

/**
 * @author Olivier Bazoud
 */
public class Maps {
    public static <K, V> ImmutableMap<K, V> transformKeys(
        Map<K, V> map, Function<K, K> keyFunction) {
        ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
        Iterator<K> iterator = map.keySet().iterator();
        while (iterator.hasNext()) {
            K key = iterator.next();
            builder.put(keyFunction.apply(key), map.get(key));
        }
        return builder.build();
    }

}
