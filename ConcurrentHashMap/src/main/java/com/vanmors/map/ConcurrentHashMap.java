package com.vanmors.map;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;


public class ConcurrentHashMap<K, V> implements Iterable<Map.Entry<K, V>> {
    private static final int DEFAULT_CAPACITY = 16;
    private static final int DEFAULT_SEGMENTS = 16;
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final float LOAD_FACTOR = 0.75f;

    private volatile Node<K, V>[] table;
    private final Segment[] segments;
    private final LongAdder[] counters;

    private final ReentrantLock lock = new ReentrantLock();

    private static final VarHandle NODE_ARRAY_HANDLE;

    private static class Segment {
        final ReentrantLock lock = new ReentrantLock();
    }

    static {
        try {
            NODE_ARRAY_HANDLE = MethodHandles.arrayElementVarHandle(Node[].class);
        } catch (final Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public ConcurrentHashMap() {
        this(DEFAULT_CAPACITY);
    }

    public ConcurrentHashMap(final int initialCapacity) {
        final int capacity = tableSizeFor(Math.max(initialCapacity, DEFAULT_SEGMENTS));
        this.table = new Node[capacity];
        this.segments = new Segment[DEFAULT_SEGMENTS];
        this.counters = new LongAdder[DEFAULT_SEGMENTS];

        for (int i = 0; i < segments.length; i++) {
            segments[i] = new Segment();
            counters[i] = new LongAdder();
        }
    }

    static class Node<K, V> {
        final int hash;

        final K key;

        volatile V value;

        Node<K, V> next;

        Node(final int hash, final K key, final V value) {
            this.hash = hash;
            this.key = key;
            this.value = value;
        }

        Node(final int hash, final K key, final V value, final Node<K, V> next) {
            this(hash, key, value);
            this.next = next;
        }
    }

    public V get(final K key) {
        final Node<K,V>[] tab = table;
        if (tab == null) return null;
        final int hash = spread(key.hashCode());

        Node<K,V> node = tabAt(tab, hash & (tab.length - 1));

        while (node != null) {
            if (node.hash == hash && key.equals(node.key)) {
                return node.value;
            }
            node = node.next;
        }
        return null;
    }

    public V getOrDefault(final K key, final V defaultValue) {
        final V value = get(key);
        return value != null ? value : defaultValue;
    }

    public V put(final K key, final V value) {
        if (key == null || value == null) throw new NullPointerException();
        final int hash = spread(key.hashCode());
        final int segmentIndex = hash & (segments.length - 1);
        final Segment segment = segments[segmentIndex];

        V oldValue = null;
        boolean added = false;

        segment.lock.lock();
        try {
            if (table == null) {
                table = new Node[DEFAULT_CAPACITY];
            }
            final int index = hash & (table.length - 1);
            Node<K, V> node = tabAt(table, index);

            while (node != null) {
                if (node.hash == hash && node.key.equals(key)) {
                    oldValue = node.value;
                    node.value = value;
                    return oldValue;
                }
                node = node.next;
            }

            final Node<K, V> newNode = new Node<>(hash, key, value, tabAt(table, index));
            setTabAt(table, index, newNode);
            incrementSize(segmentIndex);
            added = true;
        } finally {
            segment.lock.unlock();
        }

        if (added) {
            checkResize();
        }

        return oldValue;
    }

    public V merge(final K key, final V value, final BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (key == null || value == null || remappingFunction == null)
            throw new NullPointerException();

        final int hash = spread(key.hashCode());
        final int segmentIndex = hash & (segments.length - 1);
        final Segment segment = segments[segmentIndex];

        V newValue;
        boolean added = false;

        segment.lock.lock();
        try {
            if (table == null) {
                table = new Node[DEFAULT_CAPACITY];
            }

            final int index = hash & (table.length - 1);
            Node<K, V> node = tabAt(table, index);

            while (node != null) {
                if (node.hash == hash && key.equals(node.key)) {
                    newValue = remappingFunction.apply(node.value, value);
                    node.value = newValue;
                    return newValue;
                }
                node = node.next;
            }

            // Ключа не было — добавляем новый
            newValue = value;
            final Node<K, V> newNode = new Node<>(hash, key, value, tabAt(table, index));
            setTabAt(table, index, newNode);
            incrementSize(segmentIndex);
            added = true;
        } finally {
            segment.lock.unlock();
        }

        if (added) {
            checkResize();
        }

        return newValue;
    }

    public void clear() {
        for (final Segment s : segments) {
            s.lock.lock();
        }
        try {
            final Node<K, V>[] tab = table;
            for (int i = 0; i < tab.length; i++) {
                setTabAt(tab, i, null);
            }
            for (final LongAdder counter : counters) {
                counter.reset();
            }
        } finally {
            for (final Segment s : segments) {
                s.lock.unlock();
            }
        }
    }

    public long size() {
        long count = 0;
        for (final LongAdder counter: counters) {
            count += counter.sum();
        }
        return count;
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        return new EntryIterator<>(table);
    }

    private static class EntryIterator<K, V> implements Iterator<Map.Entry<K, V>> {
        private final Node<K, V>[] table;
        private int bucketIndex;
        private Node<K, V> nextNode;

        EntryIterator(final Node<K, V>[] table) {
            this.table = table;
            this.bucketIndex = 0;
            advance();
        }

        private void advance() {
            if (table == null) return;
            while (nextNode == null && bucketIndex < table.length) {
                nextNode = tabAt(table, bucketIndex++);
            }
        }

        @Override
        public boolean hasNext() {
            return nextNode != null;
        }

        @Override
        public Map.Entry<K, V> next() {
            if (nextNode == null) throw new NoSuchElementException();
            final Node<K, V> node = nextNode;
            final Map.Entry<K, V> entry = new AbstractMap.SimpleImmutableEntry<>(node.key, node.value);
            nextNode = node.next;
            if (nextNode == null) {
                advance();
            }
            return entry;
        }
    }

    @SuppressWarnings("unchecked")
    private static <K,V> Node<K,V> tabAt(final Node<K, V>[] table, final int i) {
        return (Node<K,V>) NODE_ARRAY_HANDLE.getAcquire(table, i);
    }

    private static <K,V> boolean casTabAt(final Node<K, V>[] table, final int i, final Node<K, V> expected, final Node<K, V> update) {
        return NODE_ARRAY_HANDLE.compareAndSet(table, i, expected, update);
    }

    private static <K,V> void setTabAt(final Node<K, V>[] table, final int i, final Node<K, V> value) {
        NODE_ARRAY_HANDLE.setRelease(table, i, value);
    }

    private void checkResize() {
        final Node<K, V>[] tab = table;
        if (tab != null && size() >= (long) (tab.length * LOAD_FACTOR)) {
            resize();
        }
    }

    private void resize() {
        lock.lock();
        try {
            final Node<K, V>[] oldTable = table;
            if (oldTable == null) return;
            final int oldCapacity = oldTable.length;
            // Повторная проверка: другой поток мог уже расширить
            if (size() < (long) (oldCapacity * LOAD_FACTOR) || oldCapacity >= MAXIMUM_CAPACITY) {
                return;
            }
            final int newCapacity = oldCapacity << 1;

            // Захватываем все сегменты, чтобы никто не писал во время переноса
            for (final Segment s : segments) {
                s.lock.lock();
            }
            try {
                // Ещё раз проверяем — таблица могла смениться между global lock и segment locks
                if (table != oldTable) {
                    return;
                }

                final Node<K, V>[] newTable = new Node[newCapacity];

                for (int i = 0; i < oldCapacity; i++) {
                    Node<K, V> node = tabAt(oldTable, i);
                    while (node != null) {
                        final Node<K, V> next = node.next;
                        final int newIndex = node.hash & (newCapacity - 1);
                        node.next = tabAt(newTable, newIndex);
                        setTabAt(newTable, newIndex, node);
                        node = next;
                    }
                }

                table = newTable;
            } finally {
                for (final Segment s : segments) {
                    s.lock.unlock();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private static int spread(final int h) {
        return (h ^ (h >>> 16)) & 0x7fffffff;
    }

    private static int tableSizeFor(final int c) {
        final int n = -1 >>> Integer.numberOfLeadingZeros(c - 1);
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    private void incrementSize(final int segmentIndex) {
        counters[segmentIndex].increment();
    }

}
