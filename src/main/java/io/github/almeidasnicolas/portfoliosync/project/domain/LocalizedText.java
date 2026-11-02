package io.github.almeidasnicolas.portfoliosync.project.domain;

import io.github.almeidasnicolas.portfoliosync.shared.exception.UnsupportedLanguageException;

public record LocalizedText(String es, String en) {
    public String in(String lang) {
        return switch (lang) {
            case "es" -> es;
            case "en" -> en;
            default -> throw new UnsupportedLanguageException("Unsupported language: " + lang);
        };
    }
}
