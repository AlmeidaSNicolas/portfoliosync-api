package io.github.almeidasnicolas.portfoliosync.webhook;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HmacValidatorTest {
    private static final String SECRET = "It's a Secret to Everybody";
    private static final byte[] BODY = "Hello, World!".getBytes(StandardCharsets.UTF_8);
    private static final String HEADER = "sha256=757107ea0eb2509fc211221cce984b8a37570b6d7586c22c46f4379c8b043e17";

    private final HmacValidator validator = new HmacValidator();

    @Test
    void shouldAcceptOfficialGithubExample() {

        assertTrue(validator.isAuthentic(SECRET, BODY, HEADER));

    }

    @Test
    void shouldRejectAlteredBody(){
        assertFalse(validator.isAuthentic(SECRET, "Hello, World".getBytes(StandardCharsets.UTF_8), HEADER));
    }

    @Test
    void shouldRejectWrongSecret(){
        assertFalse(validator.isAuthentic("another",BODY, HEADER));
    }

    @Test
    void shouldRejectMissingHeader(){
        assertFalse(validator.isAuthentic(SECRET, BODY, null));
    }

    @Test
    void shouldRejectHeaderWithoutPrefix(){
        assertFalse(validator.isAuthentic(SECRET, BODY, "757107ea0eb2509fc211221cce984b8a37570b6d7586c22c46f4379c8b043e17"));
    }

    @Test
    void shouldRejectMalformedSignature(){
        assertFalse(validator.isAuthentic(SECRET, BODY, "sha256=abc"));
    }
}
