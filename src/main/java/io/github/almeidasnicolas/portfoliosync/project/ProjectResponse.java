package io.github.almeidasnicolas.portfoliosync.project;

import java.util.List;

public record ProjectResponse(String repoName, String title, String description, List<String> stack, String htmlUrl, String demoUrl, boolean featured) {



}
