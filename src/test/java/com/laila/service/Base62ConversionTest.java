package com.laila.service;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Base62ConversionTest {

    private static final Pattern BASE62 = Pattern.compile("^[0-9A-Za-z]+$");
    private static final int BASE = 62;

    @Test
    void length_is_as_requested() {
        String s = Base62Generator.randomCode(8);
        assertEquals(8, s.length());
    }

    @Test
    void only_base62_characters() {
        String s = Base62Generator.randomCode(16);
        assertTrue(BASE62.matcher(s).matches(),
                "code contains non-Base62 characters: " + s);
    }

    @Test
    void deterministic_with_injected_rng() {
        Random seeded = new Random(123456789L);
        String a = Base62Generator.randomCode(12, seeded);

        // Recreate the same seed → must produce the same output
        String b = Base62Generator.randomCode(12, new Random(123456789L));

        assertEquals(a, b, "Injected RNG should make output deterministic");
    }

    @Test
    void maps_indices_to_expected_alphabet() {
        // Custom RNG that returns fixed indices: 0, 10, 61 -> '0', 'A', 'z'
        class Fixed extends Random {
            private final int[] vals = {0, 10, 61};
            private int i = 0;
            @Override public int nextInt(int bound) {
                assertEquals(BASE, bound);
                return vals[i++ % vals.length];
            }
        }
        String s = Base62Generator.randomCode(3, new Fixed());
        assertEquals("0Az", s);
    }

    @Test
    void large_sample_has_no_duplicates() {
        // Probabilistic smoke test: with 10_000 codes of length 8,
        // a collision is astronomically unlikely. This is safe in practice.
        int n = 10_000;
        Set<String> set = new java.util.HashSet<>(n * 2);
        for (int i = 0; i < n; i++) {
            String code = Base62Generator.randomCode(8);
            assertTrue(set.add(code), "duplicate detected: " + code);
        }
        assertEquals(n, set.size());
    }

    @Test
    void thread_safety_smoke_test() throws Exception {
        int threads = 6;
        int perThread = 2_000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        Set<String> set = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());

        Callable<Void> task = () -> {
            for (int i = 0; i < perThread; i++) {
                String code = Base62Generator.randomCode(8);
                assertTrue(set.add(code), "duplicate detected in concurrency test: " + code);
            }
            return null;
        };

        var futures = new java.util.ArrayList<Future<?>>();
        for (int t = 0; t < threads; t++) futures.add(pool.submit(task));
        for (Future<?> f : futures) f.get(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(threads * perThread, set.size());
    }
}
