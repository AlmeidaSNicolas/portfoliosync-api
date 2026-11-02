package io.github.almeidasnicolas.portfoliosync.project.domain;

public record LocalizedText(String es, String en) {
    public String in(String lang) {
        return switch (lang) {
            case "es" -> es;
            case "en" -> en;
            default -> throw new IllegalArgumentException("Unsupported language : " + lang);
        };
    }
}
