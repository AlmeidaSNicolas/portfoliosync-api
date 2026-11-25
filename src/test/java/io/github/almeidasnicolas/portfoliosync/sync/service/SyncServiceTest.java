package io.github.almeidasnicolas.portfoliosync.sync.service;

import io.github.almeidasnicolas.portfoliosync.TestcontainersConfiguration;
import io.github.almeidasnicolas.portfoliosync.github.client.GithubClient;
import io.github.almeidasnicolas.portfoliosync.github.dto.PortfolioFile;
import io.github.almeidasnicolas.portfoliosync.github.dto.RepoMetadata;
import io.github.almeidasnicolas.portfoliosync.github.exception.InvalidPortfolioFileException;
import io.github.almeidasnicolas.portfoliosync.github.exception.PortfolioFileNotFoundException;
import io.github.almeidasnicolas.portfoliosync.project.domain.LocalizedText;
import io.github.almeidasnicolas.portfoliosync.project.domain.Project;
import io.github.almeidasnicolas.portfoliosync.project.repository.ProjectRepository;
import io.github.almeidasnicolas.portfoliosync.sync.dto.SyncSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;


@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {"github.webhook.secret=qualquerTeste", "admin.token.secret=qualquerTeste"})
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

    @Test
    void shouldHideExistingProjectWhenRepoLosesPortfolioTopic() {
        // given
        String repoName = "teste";
        when(githubClient.getRepoMetadata(repoName)).thenReturn(metadata(repoName, List.of("portfolio")));
        when(githubClient.getPortfolioFile(repoName)).thenReturn(validFile("old title"));
        syncService.sync(repoName);

        when(githubClient.getRepoMetadata(repoName)).thenReturn(metadata(repoName, List.of("java")));

        // when
        syncService.sync(repoName);

        // then
        Project saved = projectRepository.findByGithubRepoId(1L).orElseThrow();
        assertFalse(saved.isVisible());
        assertEquals("old title", saved.getTitle().en());
    }

    @Test
    void shouldHideProjectAndRegisterErrorWhenPortfolioFileIsMissing() {
        // given
        String repoName = "teste";
        when(githubClient.getRepoMetadata(repoName)).thenReturn(metadata(repoName, List.of("portfolio")));
        when(githubClient.getPortfolioFile(repoName)).thenReturn(validFile("old title"));
        syncService.sync(repoName);

        when(githubClient.getPortfolioFile(repoName))
                .thenThrow(new PortfolioFileNotFoundException("portfolio.json not found in repo: teste", null));

        // when
        assertThrows(PortfolioFileNotFoundException.class, () -> syncService.sync(repoName));

        // then
        Project saved = projectRepository.findByGithubRepoId(1L).orElseThrow();
        assertFalse(saved.isVisible());
        assertEquals("portfolio.json not found in repo: teste", saved.getLastError());
        assertEquals("old title", saved.getTitle().en());
    }

    @Test
    void shouldHideProjectAndRegisterErrorWhenPortfolioFileIsInvalid() {
        // given
        String repoName = "teste";
        when(githubClient.getRepoMetadata(repoName)).thenReturn(metadata(repoName, List.of("portfolio")));
        when(githubClient.getPortfolioFile(repoName)).thenReturn(validFile("old title"));
        syncService.sync(repoName);

        PortfolioFile withoutEnglishTitle = new PortfolioFile(new LocalizedText("título", " "),
                new LocalizedText("description es", "description EN"),
                List.of("java"),
                "urlDemonstration.com",
                true,
                1
        );
        when(githubClient.getPortfolioFile(repoName)).thenReturn(withoutEnglishTitle);

        // when
        assertThrows(InvalidPortfolioFileException.class, () -> syncService.sync(repoName));

        // then
        Project saved = projectRepository.findByGithubRepoId(1L).orElseThrow();
        assertFalse(saved.isVisible());
        assertEquals("Title.en is required", saved.getLastError());
        assertEquals("old title", saved.getTitle().en());
    }

    @Test
    void shouldShowProjectAgainAndClearErrorWhenFileBecomesValid() {
        // given
        String repoName = "teste";
        when(githubClient.getRepoMetadata(repoName)).thenReturn(metadata(repoName, List.of("portfolio")));
        when(githubClient.getPortfolioFile(repoName)).thenReturn(validFile("old title"));
        syncService.sync(repoName);

        when(githubClient.getPortfolioFile(repoName))
                .thenThrow(new PortfolioFileNotFoundException("portfolio.json not found in repo: teste", null));
        assertThrows(PortfolioFileNotFoundException.class, () -> syncService.sync(repoName));

        doReturn(validFile("fixed title")).when(githubClient).getPortfolioFile(repoName);

        // when
        syncService.sync(repoName);

        // then
        Project saved = projectRepository.findByGithubRepoId(1L).orElseThrow();
        assertTrue(saved.isVisible());
        assertNull(saved.getLastError());
        assertEquals("fixed title", saved.getTitle().en());
    }

    @Test
    void shouldContinueBatchAndCountFailureWhenOneRepoBreaks() {
        // given
        when(githubClient.getPortfolioFile("a")).thenReturn(validFile("title a"));
        when(githubClient.getPortfolioFile("b")).thenThrow(new RuntimeException("boom"));
        when(githubClient.getPortfolioFile("c")).thenReturn(validFile("title c"));

        // when
        SyncSummary summary = syncService.syncAll(List.of(
                repo(1L, "a"), repo(2L, "b"), repo(3L, "c")));

        // then
        assertEquals(3, summary.total());
        assertEquals(2, summary.succeeded());
        assertEquals(1, summary.failed());
        assertEquals(1, summary.failures().size());
        assertEquals("b", summary.failures().get(0).repoName());
        assertEquals("boom", summary.failures().get(0).message());
        assertEquals(2, projectRepository.count());
    }

    @Test
    void shouldHideNothingWhenListingFails() {
        // given
        when(githubClient.getPortfolioFile("a")).thenReturn(validFile("title a"));
        syncService.syncAll(List.of(repo(1L, "a")));
        when(githubClient.listPortfolioRepos()).thenThrow(new RuntimeException("github down"));

        // when
        assertThrows(RuntimeException.class, () -> syncService.resyncAll());

        // then
        assertTrue(projectRepository.findByGithubRepoId(1L).orElseThrow().isVisible());
    }

    @Test
    void shouldHideProjectThatIsNoLongerInGithubListing() {
        // given
        when(githubClient.getPortfolioFile("a")).thenReturn(validFile("title a"));
        when(githubClient.getPortfolioFile("b")).thenReturn(validFile("title b"));
        syncService.syncAll(List.of(repo(1L, "a"), repo(2L, "b")));
        when(githubClient.listPortfolioRepos()).thenReturn(List.of(repo(1L, "a")));

        // when
        SyncSummary summary = syncService.resyncAll();

        // then
        assertEquals(1, summary.total());
        assertTrue(projectRepository.findByGithubRepoId(1L).orElseThrow().isVisible());
        assertFalse(projectRepository.findByGithubRepoId(2L).orElseThrow().isVisible());
    }

    @Test
    void shouldHideNothingWhenGithubListingIsEmpty() {
        // given
        when(githubClient.getPortfolioFile("a")).thenReturn(validFile("title a"));
        syncService.syncAll(List.of(repo(1L, "a")));
        when(githubClient.listPortfolioRepos()).thenReturn(List.of());

        // when
        SyncSummary summary = syncService.resyncAll();

        // then
        assertEquals(0, summary.total());
        assertTrue(projectRepository.findByGithubRepoId(1L).orElseThrow().isVisible());
    }

    private RepoMetadata repo(long id, String name) {
        return new RepoMetadata(id, name, "urlTest", List.of("portfolio"), Instant.now());
    }

    private RepoMetadata metadata(String repoName, List<String> topics) {
        return new RepoMetadata(1L, repoName, "urlTest", topics, Instant.now());
    }

    private PortfolioFile validFile(String titleEn) {
        return new PortfolioFile(new LocalizedText("título", titleEn),
                new LocalizedText("description es", "description EN"),
                List.of("java"),
                "urlDemonstration.com",
                true,
                1
        );
    }

}
