package com.vanmors.map.jcstress;

import com.vanmors.map.ConcurrentHashMap;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.I_Result;
import org.openjdk.jcstress.infra.results.II_Result;

public class ConcurrentPutStressTest {

    /**
     * Два потока пишут разные значения по одному ключу.
     * Арбитр должен увидеть одно из двух значений - никогда null и никогда повреждённые данные.
     */
    @JCStressTest
    @Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Saw actor1's value")
    @Outcome(id = "2", expect = Expect.ACCEPTABLE, desc = "Saw actor2's value")
    @State
    public static class PutPutSameKey {

        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

        @Actor
        public void actor1() {
            map.put("key", 1);
        }

        @Actor
        public void actor2() {
            map.put("key", 2);
        }

        @Arbiter
        public void arbiter(I_Result r) {
            Integer v = map.get("key");
            r.r1 = (v == null) ? -1 : v;
        }
    }

    /**
     * Два потока пишут по разным ключам.
     * Арбитр должен увидеть оба значения - проверка, что запись в разные бакеты не теряется.
     */
    @JCStressTest
    @Outcome(id = "1, 2", expect = Expect.ACCEPTABLE, desc = "Both values visible")
    @State
    public static class PutPutDifferentKeys {

        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

        @Actor
        public void actor1() {
            map.put("alpha", 1);
        }

        @Actor
        public void actor2() {
            map.put("beta", 2);
        }

        @Arbiter
        public void arbiter(II_Result r) {
            Integer v1 = map.get("alpha");
            Integer v2 = map.get("beta");
            r.r1 = (v1 == null) ? -1 : v1;
            r.r2 = (v2 == null) ? -1 : v2;
        }
    }

    /**
     * Два потока перезаписывают один ключ.
     * Арбитр проверяет, что финальное значение - одно из записанных.
     */
    @JCStressTest
    @Outcome(id = "10", expect = Expect.ACCEPTABLE, desc = "Last write from actor1")
    @Outcome(id = "20", expect = Expect.ACCEPTABLE, desc = "Last write from actor2")
    @State
    public static class PutPutOverwrite {

        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

        @Actor
        public void actor1() {
            map.put("key", 10);
        }

        @Actor
        public void actor2() {
            map.put("key", 20);
        }

        @Arbiter
        public void arbiter(I_Result r) {
            Integer v = map.get("key");
            r.r1 = (v == null) ? -1 : v;
        }
    }
}
