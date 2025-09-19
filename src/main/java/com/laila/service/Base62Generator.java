package com.laila.service;

import java.security.SecureRandom;
import java.util.Random;


public final class Base62Generator {
    private static final String ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom RNG = new SecureRandom();

    private Base62Generator() {}

    /** Production path: SecureRandom-backed */
    public static String randomCode(int length) {
        return randomCode(length, RNG);
    }

    /** Testable path: injectable RNG */
    static String randomCode(int length, Random rng) {
        if (length <= 0) throw new IllegalArgumentException("length must be > 0");
        char[] out = new char[length];
        for (int i = 0; i < length; i++) {
            out[i] = ALPHABET.charAt(rng.nextInt(ALPHABET.length()));
        }
        return new String(out);
    }
}

