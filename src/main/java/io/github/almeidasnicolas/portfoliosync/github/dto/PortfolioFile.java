package io.github.almeidasnicolas.portfoliosync.github.dto;

import io.github.almeidasnicolas.portfoliosync.project.domain.LocalizedText;

import java.util.List;

public record PortfolioFile(LocalizedText title, LocalizedText description, List<String> stack, String demoUrl, Boolean featured, Integer order) {

}
