package com.vanmors.map;

import org.instancio.Instancio;
import org.instancio.Select;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5, time = 4)
@Fork(value = 3, jvmArgs = {"-Xms2g", "-Xmx4g"})
public class BenchmarkConcurrentMap {

    private ConcurrentHashMap<String, Integer> concurrentMap;

    private HashMap<String, Integer> map;

    @Param({"100000", "500000", "1000000"})
    private int size;

    private List<String> keys;

    private List<Integer> values;

    @Setup(Level.Trial)
    public void setup() {
        map = new HashMap<>();
        concurrentMap = new ConcurrentHashMap<>();
        keys = Instancio.ofList(String.class)
                .size(size)
                .generate(Select.allStrings(), gen -> gen.string().length(20, 80))
                .create();

        values = Instancio.ofList(Integer.class)
                .size(size)
                .generate(Select.allInts(), gen -> gen.ints().max(Integer.MAX_VALUE))
                .create();

        for (int i = 0; i < size; i++) {
            map.put(keys.get(i), values.get(i));
            concurrentMap.put(keys.get(i), values.get(i));
        }
    }

    @Benchmark
    public void getConcurrentPrefTest(final Blackhole bc) {
        final int randomKey = ThreadLocalRandom.current().nextInt(0, size);
        final var result = concurrentMap.get(keys.get(randomKey));
        bc.consume(result);
    }

    @Benchmark
    public void getPrefTest(final Blackhole bc) {
        final int randomKey = ThreadLocalRandom.current().nextInt(0, size);
        final var result = map.get(keys.get(randomKey));
        bc.consume(result);
    }

    @Benchmark
    public void putConcurrentPrefTest(final Blackhole bc) {
        final String key = Instancio.of(String.class).create();
        final int value = Instancio.of(Integer.class).create();
        final var result = concurrentMap.put(key, value);
        bc.consume(result);
    }

    @Benchmark
    public void putPrefTest(final Blackhole bc) {
        final String key = Instancio.of(String.class).create();
        final int value = Instancio.of(Integer.class).create();
        final var result = map.put(key, value);
        bc.consume(result);
    }

    public static void main(String[] args) throws RunnerException {
        final Options opt = new OptionsBuilder()
                .include(BenchmarkConcurrentMap.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

}
