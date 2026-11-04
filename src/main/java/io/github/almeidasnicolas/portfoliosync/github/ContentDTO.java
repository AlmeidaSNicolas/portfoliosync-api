package io.github.almeidasnicolas.portfoliosync.github;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public record ContentDTO(String content) {

    public String decode() {

        byte[] decode = Base64.getMimeDecoder().decode(content.getBytes(StandardCharsets.UTF_8));

        String decodedString = new String(decode, StandardCharsets.UTF_8);

        return decodedString;
    }
}