package io.github.almeidasnicolas.portfoliosync.project;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@Profile("dev")
public class ProjectSeeder implements CommandLineRunner {
    private final ProjectRepository projectRepository;

    @Override
    public void run(String... args) throws Exception {
        if (projectRepository.count() > 0){
            return;
        }
        Project project1 = new Project(
                1L,                                   // githubRepoId
                "geopolitica-news-bot",               // repoName
                "https://github.com/...",             // htmlUrl
                new LocalizedText("...", "..."),      // title       (es, en)
                new LocalizedText("...", "..."),      // description (es, en)
                List.of("Java", "PostgreSQL"),        // stack
                null,                                 // demoUrl
                true,                                 // featured
                1,                                    // displayOrder
                Instant.now()                         // pushedAt
        );

        Project project2 = new Project(
                2L,
                "ApiPortfolioSync",
                "https://github.com/...",
                new LocalizedText("...", "..."),
                new LocalizedText("...", "..."),
                List.of("Java", "Spring", "SpringJpa", "Hibernate", "PostgreSQL", "CI", "GitHubActions"),
                null,
                true,
                2,
                Instant.now()
        );

        Project project3 = new Project(
                3L,
                "AirPortApi",
                "https://github.com/...",
                new LocalizedText("...", "..."),
                new LocalizedText("...", "..."),
                List.of("Typescript", "Express", "Node", "PostgreSQL"),
                null,
                false,
                null,
                Instant.now()
        );

        projectRepository.saveAll(List.of(project1, project2, project3));

    }

    public ProjectSeeder(ProjectRepository projectRepository){
        this.projectRepository = projectRepository;
    }
}
