package io.github.almeidasnicolas.portfoliosync.sync.dto;

import java.util.List;

public record SyncSummary(int total, int succeeded, int failed, List<SyncFailure> failures) {
}
