package io.github.almeidasnicolas.portfoliosync.sync.service;

import io.github.almeidasnicolas.portfoliosync.TestcontainersConfiguration;
import io.github.almeidasnicolas.portfoliosync.github.client.GithubClient;
import io.github.almeidasnicolas.portfoliosync.github.dto.PortfolioFile;
import io.github.almeidasnicolas.portfoliosync.github.dto.RepoMetadata;
import io.github.almeidasnicolas.portfoliosync.project.domain.LocalizedText;
import io.github.almeidasnicolas.portfoliosync.project.domain.Project;
import io.github.almeidasnicolas.portfoliosync.project.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;


@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "github.webhook.secret=qualquerTeste")
public class SyncServiceTest {

    @MockitoBean
    GithubClient githubClient;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    SyncService syncService;


    @BeforeEach
    void cleanDatabase(){
        projectRepository.deleteAll();
    }


    @Test
    void shouldCreateProjectWhenRepoIsNew() {

        String repoName = "teste";

        RepoMetadata repoMetadata = new RepoMetadata(1L,
                "teste",
                "urlTest",
                List.of("portfolio", "java", "docker"),
                Instant.now());

        PortfolioFile portfolioFile = new PortfolioFile(new LocalizedText("espanol", "english"),
                new LocalizedText("description es", "description EN"),
                List.of("java", "spring", "docker"),
                "urlDemonstration.com",
                true,
                1
        );

        //given
        when(githubClient.getRepoMetadata(repoName)).thenReturn(repoMetadata);
        when(githubClient.getPortfolioFile(repoName)).thenReturn(portfolioFile);

        // when
        syncService.sync(repoName);

        // then
        assertEquals(1,projectRepository.count());
    }

    @Test
    void shouldUpdateExistingProjectInsteadOfDuplicating() {
        // given
        String repoName = "teste";

        RepoMetadata repoMetadata = new RepoMetadata(1L,
                repoName,
                "urlTest",
                List.of("portfolio", "java"),
                Instant.now());

        PortfolioFile firstVersion = new PortfolioFile(new LocalizedText("título viejo", "old title"),
                new LocalizedText("description es", "description EN"),
                List.of("java"),
                "urlDemonstration.com",
                true,
                1
        );

        PortfolioFile secondVersion = new PortfolioFile(new LocalizedText("título nuevo", "new title"),
                new LocalizedText("description es", "description EN"),
                List.of("java"),
                "urlDemonstration.com",
                true,
                1
        );

        when(githubClient.getRepoMetadata(repoName)).thenReturn(repoMetadata);
        when(githubClient.getPortfolioFile(repoName)).thenReturn(firstVersion);
        syncService.sync(repoName);

        when(githubClient.getPortfolioFile(repoName)).thenReturn(secondVersion);

        // when
        syncService.sync(repoName);

        // then
        assertEquals(1, projectRepository.count());
        Project saved = projectRepository.findByGithubRepoId(1L).orElseThrow();
        assertEquals("new title", saved.getTitle().en());
        assertEquals("título nuevo", saved.getTitle().es());
    }

    @Test
    void shouldIgnoreRepoWithoutPortfolioTopic() {
        // given
        String repoName = "teste";

        RepoMetadata repoMetadata = new RepoMetadata(1L,
                repoName,
                "urlTest",
                List.of("java", "docker"),
                Instant.now());

        when(githubClient.getRepoMetadata(repoName)).thenReturn(repoMetadata);

        // when
        syncService.sync(repoName);

        // then
        assertEquals(0, projectRepository.count());
        verify(githubClient, never()).getPortfolioFile(repoName);
    }

}
