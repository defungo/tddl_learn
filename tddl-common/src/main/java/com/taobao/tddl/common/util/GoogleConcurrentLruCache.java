package com.taobao.tddl.common.util;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.Weighers;

public class GoogleConcurrentLruCache<K, V> {
	private ConcurrentLinkedHashMap<K, V> cache;
	private static final int DEFAULT_CAPACITY = 389;
	public static final int DEFAULT_CONCURENCY_LEVEL = 64;
	private AtomicLong get = new AtomicLong(0);
	private AtomicLong hit = new AtomicLong(0);

	public GoogleConcurrentLruCache() {
		this(DEFAULT_CAPACITY);
	}

	public GoogleConcurrentLruCache(int capacity) {
		if (capacity <= 0) {
			capacity = DEFAULT_CAPACITY;
		}

		cache = new ConcurrentLinkedHashMap.Builder<K, V>()
				.maximumWeightedCapacity(capacity)
				.weigher(Weighers.singleton())
				.concurrencyLevel(DEFAULT_CONCURENCY_LEVEL).build();
	}

	public long capacity() {
		return cache.capacity();
	}

	public boolean isEmpty() {
		return cache.isEmpty();
	}

	public int size() {
		return cache.size();
	}

	public void clear() {
		cache.clear();
	}

	public V get(Object key) {
		V v = cache.get(key);
		get.addAndGet(1);
		if (v != null) {
			hit.addAndGet(1);
		}

		return v;
	}

	public void put(K key, V value) {
		cache.put(key, value);
	}

	public boolean putIfAbsent(K key, V value) {
		return cache.putIfAbsent(key, value) == null;
	}

	public boolean replace(K key, V old, V value) {
		return cache.replace(key, old, value);
	}

	public void remove(K key) {
		cache.remove(key);
	}

	public Set<K> keySet() {
		return cache.keySet();
	}

	public Set<K> hotKeySet(int n) {
		return cache.descendingKeySetWithLimit(n);
	}

	public boolean containsKey(K key) {
		return cache.containsKey(key);
	}

	public String getStatus() {
		StringBuilder sb = new StringBuilder();
		sb.append("current size:");
		sb.append(cache.size());
		sb.append(" get:");
		sb.append(get);
		sb.append(" hit:");
		sb.append(hit);
		sb.append(" hit ratio:");
		if (get.longValue() > 0) {
			sb.append((hit.doubleValue() / get.doubleValue())*100);
			sb.append("%");
		}else{
			sb.append("--");
		}
		
		get.set(0);
		hit.set(0);
		
		return sb.toString();
	}
}
