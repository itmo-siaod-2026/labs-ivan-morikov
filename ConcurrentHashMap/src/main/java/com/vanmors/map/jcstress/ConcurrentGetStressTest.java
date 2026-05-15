package com.vanmors.map.jcstress;

import com.vanmors.map.ConcurrentHashMap;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.I_Result;
import org.openjdk.jcstress.infra.results.II_Result;

public class ConcurrentGetStressTest {

    /**
     * Один поток пишет, другой читает тот же ключ.
     * Читатель должен увидеть либо null (-1), либо записанное значение - никогда мусор.
     */
    @JCStressTest
    @Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "Read before put")
    @Outcome(id = "42", expect = Expect.ACCEPTABLE, desc = "Read after put")
    @State
    public static class PutGet {

        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

        @Actor
        public void writer() {
            map.put("key", 42);
        }

        @Actor
        public void reader(I_Result r) {
            Integer v = map.get("key");
            r.r1 = (v == null) ? -1 : v;
        }
    }

    /**
     * Один поток перезаписывает значение (1 → 2), другой читает.
     * Читатель видит либо старое, либо новое значение - атомарность записи value.
     */
    @JCStressTest
    @Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Read old value")
    @Outcome(id = "2", expect = Expect.ACCEPTABLE, desc = "Read new value")
    @State
    public static class GetDuringOverwrite {

        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

        { map.put("key", 1); }

        @Actor
        public void writer() {
            map.put("key", 2);
        }

        @Actor
        public void reader(I_Result r) {
            r.r1 = map.get("key");
        }
    }

    /**
     * Два читателя + один писатель.
     * Оба читателя должны видеть консистентное значение (null или 99), независимо друг от друга.
     */
    @JCStressTest
    @Outcome(id = "-1, -1", expect = Expect.ACCEPTABLE, desc = "Both read before put")
    @Outcome(id = "99, -1", expect = Expect.ACCEPTABLE, desc = "Reader1 after, reader2 before")
    @Outcome(id = "-1, 99", expect = Expect.ACCEPTABLE, desc = "Reader1 before, reader2 after")
    @Outcome(id = "99, 99", expect = Expect.ACCEPTABLE, desc = "Both read after put")
    @State
    public static class TwoReadersOneWriter {

        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

        @Actor
        public void writer() {
            map.put("key", 99);
        }

        @Actor
        public void reader1(II_Result r) {
            Integer v = map.get("key");
            r.r1 = (v == null) ? -1 : v;
        }

        @Actor
        public void reader2(II_Result r) {
            Integer v = map.get("key");
            r.r2 = (v == null) ? -1 : v;
        }
    }
}
