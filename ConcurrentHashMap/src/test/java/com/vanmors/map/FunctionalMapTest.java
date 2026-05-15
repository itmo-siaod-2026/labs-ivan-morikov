package com.vanmors.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class FunctionalMapTest {


    @Test
    void putAndGet_singleEntry() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        assertNull(map.put("key", 1));
        assertEquals(1, map.get("key"));
    }

    @Test
    void get_nonExistentKey_returnsNull() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        assertNull(map.get("missing"));
    }

    @Test
    void put_multipleEntries() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");

        assertEquals("1", map.get("a"));
        assertEquals("2", map.get("b"));
        assertEquals("3", map.get("c"));
    }

    @Test
    void put_overwriteExistingKey_returnsOldValue() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        assertNull(map.put("key", 1));
        assertEquals(1, map.put("key", 2));
        assertEquals(2, map.get("key"));
    }

    @Test
    void put_nullKey_throwsNPE() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        assertThrows(NullPointerException.class, () -> map.put(null, 1));
    }

    @Test
    void put_nullValue_throwsNPE() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        assertThrows(NullPointerException.class, () -> map.put("key", null));
    }

    @Test
    void size_emptyMap() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        assertEquals(0, map.size());
    }

    @Test
    void size_afterPuts() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(3, map.size());
    }

    @Test
    void customInitialCapacity() {
        ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<>(32);
        for (int i = 0; i < 100; i++) {
            map.put(i, i * 10);
        }
        for (int i = 0; i < 100; i++) {
            assertEquals(i * 10, map.get(i));
        }
    }

    @Test
    void integerKeys() {
        ConcurrentHashMap<Integer, String> map = new ConcurrentHashMap<>();
        for (int i = 0; i < 50; i++) {
            map.put(i, "val" + i);
        }
        for (int i = 0; i < 50; i++) {
            assertEquals("val" + i, map.get(i));
        }
    }


    @Test
    void put_collidingKeys_allRetrievable() {
        // Keys with identical hashCode to force collisions in the same bucket
        ConcurrentHashMap<CollidingKey, String> map = new ConcurrentHashMap<>();
        CollidingKey k1 = new CollidingKey("a", 42);
        CollidingKey k2 = new CollidingKey("b", 42);
        CollidingKey k3 = new CollidingKey("c", 42);

        map.put(k1, "v1");
        map.put(k2, "v2");
        map.put(k3, "v3");

        assertEquals("v1", map.get(k1));
        assertEquals("v2", map.get(k2));
        assertEquals("v3", map.get(k3));
    }

    @Test
    void put_collidingKey_overwrite() {
        ConcurrentHashMap<CollidingKey, String> map = new ConcurrentHashMap<>();
        CollidingKey k1 = new CollidingKey("a", 42);

        map.put(k1, "v1");
        assertEquals("v1", map.put(k1, "v2"));
        assertEquals("v2", map.get(k1));
    }


    @Test
    void merge_absentKey_insertsValue() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        Integer result = map.merge("key", 10, Integer::sum);
        assertEquals(10, result);
        assertEquals(10, map.get("key"));
    }

    @Test
    void merge_existingKey_appliesRemappingFunction() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        map.put("key", 3);
        Integer result = map.merge("key", 7, Integer::sum);
        assertEquals(10, result);
        assertEquals(10, map.get("key"));
    }

    @Test
    void merge_existingKey_returnsRemappedValue() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
        map.put("key", "hello");
        String result = map.merge("key", " world", String::concat);
        assertEquals("hello world", result);
        assertEquals("hello world", map.get("key"));
    }

    @Test
    void merge_multipleTimes_accumulates() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        for (int i = 1; i <= 5; i++) {
            map.merge("sum", i, Integer::sum);
        }
        assertEquals(15, map.get("sum")); // 1+2+3+4+5
    }

    @Test
    void merge_nullKey_throwsNPE() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        assertThrows(NullPointerException.class, () -> map.merge(null, 1, Integer::sum));
    }

    @Test
    void merge_nullValue_throwsNPE() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        assertThrows(NullPointerException.class, () -> map.merge("key", null, Integer::sum));
    }

    @Test
    void merge_nullFunction_throwsNPE() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        assertThrows(NullPointerException.class, () -> map.merge("key", 1, null));
    }

    @Test
    void merge_absentKey_incrementsSize() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        map.merge("a", 1, Integer::sum);
        map.merge("b", 2, Integer::sum);
        assertEquals(2, map.size());
    }

    @Test
    void merge_afterPut_remapsCorrectly() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        map.put("key", 100);
        map.merge("key", 50, (oldVal, newVal) -> oldVal - newVal);
        assertEquals(50, map.get("key")); // 100 - 50
    }

    @Test
    void merge_customRemappingFunction_keepMax() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        map.put("key", 5);
        map.merge("key", 3, Integer::max);
        assertEquals(5, map.get("key"));

        map.merge("key", 10, Integer::max);
        assertEquals(10, map.get("key"));
    }


    @Test
    void merge_collidingKeys_allRemappedCorrectly() {
        ConcurrentHashMap<CollidingKey, Integer> map = new ConcurrentHashMap<>();
        CollidingKey k1 = new CollidingKey("a", 99);
        CollidingKey k2 = new CollidingKey("b", 99);

        map.put(k1, 10);
        map.put(k2, 20);

        map.merge(k1, 5, Integer::sum);
        map.merge(k2, 5, Integer::sum);

        assertEquals(15, map.get(k1));
        assertEquals(25, map.get(k2));
    }

    @Test
    void merge_collidingAbsentKey_insertsIntoBucket() {
        ConcurrentHashMap<CollidingKey, String> map = new ConcurrentHashMap<>();
        CollidingKey k1 = new CollidingKey("a", 77);
        CollidingKey k2 = new CollidingKey("b", 77);

        map.put(k1, "existing");
        map.merge(k2, "new", String::concat);

        assertEquals("existing", map.get(k1));
        assertEquals("new", map.get(k2));
    }

    // ==================== Resize tests ====================

    @Test
    void resize_triggersWhenLoadFactorExceeded() {
        // Default capacity 16, load factor 0.75 → resize at 12 elements
        ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<>(16);
        for (int i = 0; i < 50; i++) {
            map.put(i, i);
        }
        // All entries must survive multiple resizes
        for (int i = 0; i < 50; i++) {
            assertEquals(i, map.get(i), "Lost key " + i + " after resize");
        }
    }

    @Test
    void resize_preservesCollidingKeys() {
        ConcurrentHashMap<CollidingKey, Integer> map = new ConcurrentHashMap<>(16);
        // Force 20 keys into the same hash bucket, exceeding load factor
        for (int i = 0; i < 20; i++) {
            map.put(new CollidingKey("k" + i, 42), i);
        }
        for (int i = 0; i < 20; i++) {
            assertEquals(i, map.get(new CollidingKey("k" + i, 42)));
        }
    }

    @Test
    void resize_overwriteAfterResize() {
        ConcurrentHashMap<Integer, String> map = new ConcurrentHashMap<>(16);
        // Fill beyond load factor to trigger resize
        for (int i = 0; i < 50; i++) {
            map.put(i, "v" + i);
        }
        // Overwrite keys — should still find the right bucket after resize
        for (int i = 0; i < 50; i++) {
            map.put(i, "updated" + i);
        }
        for (int i = 0; i < 50; i++) {
            assertEquals("updated" + i, map.get(i));
        }
    }

    @Test
    void resize_mergeAfterResize() {
        ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<>(16);
        for (int i = 0; i < 50; i++) {
            map.put(i, 1);
        }
        // merge on keys that were rehashed
        for (int i = 0; i < 50; i++) {
            map.merge(i, 10, Integer::sum);
        }
        for (int i = 0; i < 50; i++) {
            assertEquals(11, map.get(i));
        }
    }

    // ==================== Helper ====================

    /**
     * Key class that allows controlling the hashCode to force collisions.
     */
    private static class CollidingKey {
        private final String id;

        private final int hash;

        CollidingKey(String id, int hash) {
            this.id = id;
            this.hash = hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CollidingKey other)) {
                return false;
            }
            return id.equals(other.id);
        }
    }
}
