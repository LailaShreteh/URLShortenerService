package com.laila.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class Base62Converter {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final char[] ALLOWED_CHARACTERS = ALPHABET.toCharArray();
    private static final int BASE = ALLOWED_CHARACTERS.length;
    private static final Map<Character, Integer> CHAR_INDEX = new HashMap<>(); // fast lookup map

    static {
        for (int i = 0; i < ALLOWED_CHARACTERS.length; i++) {
            CHAR_INDEX.put(ALLOWED_CHARACTERS[i], i);
        }
    }

    public String encode(long input) {
        if (input == 0) {
            return String.valueOf(ALLOWED_CHARACTERS[0]);
        }

        StringBuilder encodedString = new StringBuilder();
        while (input > 0) {
            encodedString.append(ALLOWED_CHARACTERS[(int) (input % BASE)]);
            input /= BASE;
        }
        return encodedString.reverse().toString();
    }

    public long decode(String input) {
        long decoded = 0;
        for (char c : input.toCharArray()) {
            decoded = decoded * BASE + CHAR_INDEX.get(c);
        }
        return decoded;
    }
}
