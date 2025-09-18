package com.laila.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Base62ConversionTest {

    private final Base62Converter base62Converter = new Base62Converter();

    @Test
    void encode_lessThan62() {
        assertEquals("k", base62Converter.encode(10));
    }

    @Test
    void encode_moreThan62() {
        assertEquals("bq", base62Converter.encode(78));
    }

    @Test
    void decode_singleCharacter() {
        assertEquals(11, base62Converter.decode("l"));
    }

    @Test
    void roundTrip() {
        for (long i = 0; i < 10_000; i++) {
            assertEquals(i, base62Converter.decode(base62Converter.encode(i)));
        }
    }
}
