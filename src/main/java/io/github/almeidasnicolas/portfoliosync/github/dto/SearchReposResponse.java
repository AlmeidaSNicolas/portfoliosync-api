package io.github.almeidasnicolas.portfoliosync.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


public record SearchReposResponse(@JsonProperty("total_count") int totalCount, @JsonProperty("incomplete_results") boolean incompleteResults, List<RepoMetadata> items) {
}
