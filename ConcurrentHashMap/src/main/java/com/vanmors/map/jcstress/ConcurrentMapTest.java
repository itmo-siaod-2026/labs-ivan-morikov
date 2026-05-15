package com.vanmors.map.jcstress;

import com.vanmors.map.ConcurrentHashMap;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.I_Result;

/**
 * Два потока конкурентно делают merge(+1) по одному ключу.
 * Арбитр проверяет итоговое значение.
 * Значение 2 — оба merge применились (корректно).
 * Значение 1 — один merge потерян (lost update).
 * Значение 0 — оба потеряны.
 */
@JCStressTest
@Outcome(id = "2", expect = Expect.ACCEPTABLE, desc = "Both merges applied")
@Outcome(id = "1", expect = Expect.FORBIDDEN,  desc = "Lost update — one merge lost!")
@Outcome(id = "0", expect = Expect.FORBIDDEN,  desc = "Both merges lost!")
@State
public class ConcurrentMapTest {

    ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

    @Actor
    public void actor1() {
        map.merge("key", 1, Integer::sum);
    }

    @Actor
    public void actor2() {
        map.merge("key", 1, Integer::sum);
    }

    @Arbiter
    public void arbiter(I_Result r) {
        r.r1 = map.getOrDefault("key", 0);
    }
}
