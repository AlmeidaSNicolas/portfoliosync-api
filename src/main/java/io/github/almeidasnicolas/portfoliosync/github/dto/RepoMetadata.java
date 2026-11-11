package io.github.almeidasnicolas.portfoliosync.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public record RepoMetadata(Long id, String name, @JsonProperty("html_url") String htmlUrl,
                           List<String> topics, @JsonProperty("pushed_at") Instant pushedAt) {



}
