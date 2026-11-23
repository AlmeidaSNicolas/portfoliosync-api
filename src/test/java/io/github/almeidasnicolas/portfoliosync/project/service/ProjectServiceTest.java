package io.github.almeidasnicolas.portfoliosync.project.service;

import io.github.almeidasnicolas.portfoliosync.TestcontainersConfiguration;
import io.github.almeidasnicolas.portfoliosync.project.domain.LocalizedText;
import io.github.almeidasnicolas.portfoliosync.project.domain.Project;
import io.github.almeidasnicolas.portfoliosync.project.dto.ProjectResponse;
import io.github.almeidasnicolas.portfoliosync.project.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {"github.webhook.secret=qualquerTeste", "admin.token.secret=qualquerTeste"})
class ProjectServiceTest {

    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private ProjectService projectService;
    @Autowired
    private ProjectRepository projectRepository;

    @BeforeEach
    void cleanDatabase() {
        projectRepository.deleteAll();
    }

    @Test
    void shouldListFeaturedFirstThenDisplayOrderWithNullsLastThenNewestPush() {
        // Saved on purpose in an order that is NOT the expected one,
        // so the result can only come out right if the database sorts it.
        projectRepository.saveAll(List.of(
                project(1L, "plain-old", false, null, 1),
                project(2L, "featured-null-order-new-push", true, null, 5),
                project(3L, "plain-order-1", false, 1, 1),
                project(4L, "featured-order-2", true, 2, 1),
                project(5L, "plain-new", false, null, 3),
                project(6L, "featured-order-1", true, 1, 1)
        ));

        List<String> names = projectService.listVisible("en").stream()
                .map(ProjectResponse::repoName)
                .toList();

        assertEquals(List.of(
                "featured-order-1",             // featured, displayOrder 1
                "featured-order-2",             // featured, displayOrder 2
                "featured-null-order-new-push", // featured, null displayOrder goes after the numbered ones
                "plain-order-1",                // not featured: numbered before null
                "plain-new",                    // not featured, null order: most recent push first
                "plain-old"
        ), names);
    }

    @Test
    void shouldNotListHiddenProjectsEvenWhenFeatured() {
        Project hidden = project(1L, "hidden-featured", true, 1, 1);
        hidden.hide();
        projectRepository.saveAll(List.of(hidden, project(2L, "visible-plain", false, null, 1)));

        List<String> names = projectService.listVisible("en").stream()
                .map(ProjectResponse::repoName)
                .toList();

        assertEquals(List.of("visible-plain"), names);
    }

    private Project project(Long githubRepoId, String repoName, boolean featured, Integer displayOrder, int pushedDaysAfterBase) {
        return new Project(
                githubRepoId,
                repoName,
                "https://github.com/AlmeidaSNicolas/" + repoName,
                new LocalizedText("titulo", "title"),
                new LocalizedText("descripcion", "description"),
                List.of("Java"),
                null,
                featured,
                displayOrder,
                BASE.plus(pushedDaysAfterBase, ChronoUnit.DAYS)
        );
    }
}
