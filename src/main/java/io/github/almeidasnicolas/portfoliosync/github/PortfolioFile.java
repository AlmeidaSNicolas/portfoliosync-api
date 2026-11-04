package io.github.almeidasnicolas.portfoliosync.github;

import io.github.almeidasnicolas.portfoliosync.project.domain.LocalizedText;

import java.util.List;

public record PortfolioFile(LocalizedText title, LocalizedText description, List<String> stack, String demoUrl, boolean featured, Integer order) {

}
