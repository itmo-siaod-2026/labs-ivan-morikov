package com.vanmors.map;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Многопоточные бенчмарки ConcurrentHashMap.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@Fork(value = 2, jvmArgs = {"-Xms2g", "-Xmx4g"})
public class BenchmarkMultiThread {

    private static final int SIZE = 1_000_000;

    // ==================== Shared state ====================

    @State(Scope.Benchmark)
    public static class CustomMapState {
        ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<>();

        @Setup(Level.Trial)
        public void setup() {
            for (int i = 0; i < SIZE; i++) {
                map.put(i, i);
            }
        }
    }

    @State(Scope.Benchmark)
    public static class JdkMapState {
        java.util.concurrent.ConcurrentHashMap<Integer, Integer> map =
                new java.util.concurrent.ConcurrentHashMap<>();

        @Setup(Level.Trial)
        public void setup() {
            for (int i = 0; i < SIZE; i++) {
                map.put(i, i);
            }
        }
    }

    @State(Scope.Benchmark)
    public static class HashMapState {
        HashMap<Integer, Integer> map = new HashMap<>();

        @Setup(Level.Trial)
        public void setup() {
            for (int i = 0; i < SIZE; i++) {
                map.put(i, i);
            }
        }
    }

    // ==================== 1. Thread scaling: get ====================

    @Benchmark
    public void getCustom(CustomMapState s, Blackhole bh) {
        int key = ThreadLocalRandom.current().nextInt(SIZE);
        bh.consume(s.map.get(key));
    }

    @Benchmark
    public void getJdk(JdkMapState s, Blackhole bh) {
        int key = ThreadLocalRandom.current().nextInt(SIZE);
        bh.consume(s.map.get(key));
    }

    @Benchmark
    @Threads(1) // всегда 1 поток — baseline
    public void getHashMap(HashMapState s, Blackhole bh) {
        int key = ThreadLocalRandom.current().nextInt(SIZE);
        bh.consume(s.map.get(key));
    }

    // ==================== 2. Thread scaling: put ====================

    @Benchmark
    public void putCustom(CustomMapState s, Blackhole bh) {
        int key = ThreadLocalRandom.current().nextInt(SIZE);
        bh.consume(s.map.put(key, key));
    }

    @Benchmark
    public void putJdk(JdkMapState s, Blackhole bh) {
        int key = ThreadLocalRandom.current().nextInt(SIZE);
        bh.consume(s.map.put(key, key));
    }

    @Benchmark
    @Threads(1)
    public void putHashMap(HashMapState s, Blackhole bh) {
        int key = ThreadLocalRandom.current().nextInt(SIZE);
        bh.consume(s.map.put(key, key));
    }

    public static void main(String[] args) throws RunnerException {
        // 0) HashMap baseline — строго 1 поток (HashMap не thread-safe)
        System.out.println("=== HashMap Baseline (1 thread) ===");
        Options baselineOpt = new OptionsBuilder()
                .include(BenchmarkMultiThread.class.getSimpleName()
                        + "\\.(get|put)HashMap")
                .threads(1)
                .result("results_hashmap_baseline.json")
                .resultFormat(ResultFormatType.JSON)
                .build();
        new Runner(baselineOpt).run();

        // 1) Thread scaling — get и put при 1, 2, 4, 8 потоках (только concurrent реализации)
        System.out.println("=== Thread Scaling Benchmarks ===");
        for (int threads : new int[]{1, 2, 4, 8}) {
            Options opt = new OptionsBuilder()
                    .include(BenchmarkMultiThread.class.getSimpleName()
                            + "\\.(get|put)(Custom|Jdk)")
                    .threads(threads)
                    .result("results_threads_" + threads + ".json")
                    .resultFormat(ResultFormatType.JSON)
                    .build();
            new Runner(opt).run();
        }

    }
}
