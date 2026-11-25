package io.github.almeidasnicolas.portfoliosync.github.client;

import io.github.almeidasnicolas.portfoliosync.github.dto.PortfolioFile;
import io.github.almeidasnicolas.portfoliosync.github.dto.RepoMetadata;
import io.github.almeidasnicolas.portfoliosync.github.exception.GithubListingException;
import io.github.almeidasnicolas.portfoliosync.github.exception.PortfolioFileNotFoundException;
import io.github.almeidasnicolas.portfoliosync.github.exception.RepositoryNotFoundException;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpStatusCodeException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


@WireMockTest
public class GithubClientTest {


    @Test
    void shouldReturnPortfolioFileWhenRepoExists(WireMockRuntimeInfo wm) {
        GithubClient githubClient = new GithubClient(new JsonMapper(), wm.getHttpBaseUrl());


        String portfolioJson = """
        {"title":{"es":"Hola","en":"Hello"},"order":6}
        """;

        String base64 = Base64.getEncoder().encodeToString(portfolioJson.getBytes(StandardCharsets.UTF_8));

        String envelope = "{\"content\":\"" + base64 + "\"}";

        stubFor(get(urlEqualTo("/repos/AlmeidaSNicolas/amendoinSalgado/contents/portfolio.json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(envelope)));


        PortfolioFile resultArchive = githubClient.getPortfolioFile("amendoinSalgado");

        assertEquals(6, resultArchive.order());
        assertEquals("Hello", resultArchive.title().en());

    }

    @Test
    public void shouldThrowPortfolioFileNotFoundExceptionWhenGithubReturns404(WireMockRuntimeInfo wireMockRuntimeInfo){
        GithubClient githubClient = new GithubClient(new JsonMapper(), wireMockRuntimeInfo.getHttpBaseUrl());

        stubFor(get(urlEqualTo("/repos/AlmeidaSNicolas/amendoinSalgado/contents/portfolio.json"))
                .willReturn(aResponse()
                        .withStatus(404)));

        Exception exception = assertThrows(PortfolioFileNotFoundException.class, () -> githubClient.getPortfolioFile("amendoinSalgado"));
    }

    @Test
    public void shouldThrowRepositoryNotFoundExceptionWhenGithubReturns404(WireMockRuntimeInfo wireMockRuntimeInfo){
        GithubClient githubClient = new GithubClient(new JsonMapper(), wireMockRuntimeInfo.getHttpBaseUrl());

        stubFor(get(urlEqualTo("/repos/AlmeidaSNicolas/amendoinSalgado"))
                .willReturn(aResponse()
                        .withStatus(404)));

        assertThrows(RepositoryNotFoundException.class, () -> githubClient.getRepoMetadata("amendoinSalgado"));

    }

    @Test
    public void shouldReturnRepoMetadataWhenRepoExists(WireMockRuntimeInfo wireMockRuntimeInfo){
        GithubClient githubClient = new GithubClient(new JsonMapper(), wireMockRuntimeInfo.getHttpBaseUrl());

        String repoJson = """
        {
          "id": 1200,
          "name": "amendoinSalgado",
          "html_url": "https://youtube.com",
          "topics": ["portfolio", "java"],
          "pushed_at":"2026-11-11T12:00:00Z"
        }
        """;

        stubFor(get(urlEqualTo("/repos/AlmeidaSNicolas/amendoinSalgado"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(repoJson)
                        .withHeader("Content-Type", "application/json")
                ));


        RepoMetadata amendoinSalgado = githubClient.getRepoMetadata("amendoinSalgado");

        assertEquals(1200L, amendoinSalgado.id());
        assertEquals("amendoinSalgado", amendoinSalgado.name());
        assertEquals("https://youtube.com", amendoinSalgado.htmlUrl());
        assertEquals(List.of("portfolio", "java"), amendoinSalgado.topics());
        assertEquals(Instant.parse("2026-11-11T12:00:00Z"), amendoinSalgado.pushedAt());
    }

    private String repoJson(long id) {
        return """
        {"id": %d, "name": "repo%d", "html_url": "https://example.com/%d", "topics": ["portfolio"], "pushed_at": "2026-11-11T12:00:00Z"}
        """.formatted(id, id, id);
    }

    private String[] repos(int from, int count) {
        return IntStream.range(from, from + count).mapToObj(i -> repoJson(i)).toArray(String[]::new);
    }

    private String searchBody(int total, boolean incomplete, String... items) {
        return """
        {"total_count": %d, "incomplete_results": %s, "items": [%s]}
        """.formatted(total, incomplete, String.join(",", items));
    }

    @Test
    void shouldReturnAllReposAcrossPages(WireMockRuntimeInfo wm) {
        GithubClient githubClient = new GithubClient(new JsonMapper(), wm.getHttpBaseUrl());

        stubFor(get(urlPathEqualTo("/search/repositories"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(searchBody(150, false, repos(1, 100)))));

        stubFor(get(urlPathEqualTo("/search/repositories"))
                .withQueryParam("page", equalTo("2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(searchBody(150, false, repos(101, 50)))));

        List<RepoMetadata> result = githubClient.listPortfolioRepos();

        assertEquals(150, result.size());
        assertEquals(1L, result.getFirst().id());
        assertEquals(150L, result.getLast().id());
    }

    @Test
    void shouldReturnEmptyListWhenNoReposMatch(WireMockRuntimeInfo wm) {
        GithubClient githubClient = new GithubClient(new JsonMapper(), wm.getHttpBaseUrl());

        stubFor(get(urlPathEqualTo("/search/repositories"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(searchBody(0, false))));

        assertTrue(githubClient.listPortfolioRepos().isEmpty());
    }

    @Test
    void shouldThrowWhenSearchResultIsIncomplete(WireMockRuntimeInfo wm) {
        GithubClient githubClient = new GithubClient(new JsonMapper(), wm.getHttpBaseUrl());

        stubFor(get(urlPathEqualTo("/search/repositories"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(searchBody(1, true, repos(1, 1)))));

        assertThrows(GithubListingException.class, () -> githubClient.listPortfolioRepos());
    }

    @Test
    void shouldThrowWhenPageIsEmptyBeforeReachingTotal(WireMockRuntimeInfo wm) {
        GithubClient githubClient = new GithubClient(new JsonMapper(), wm.getHttpBaseUrl());

        stubFor(get(urlPathEqualTo("/search/repositories"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(searchBody(150, false, repos(1, 100)))));

        stubFor(get(urlPathEqualTo("/search/repositories"))
                .withQueryParam("page", equalTo("2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(searchBody(150, false))));

        assertThrows(GithubListingException.class, () -> githubClient.listPortfolioRepos());
    }

    @Test
    void shouldThrowWithHttpCauseWhenSecondPageFails(WireMockRuntimeInfo wm) {
        GithubClient githubClient = new GithubClient(new JsonMapper(), wm.getHttpBaseUrl());

        stubFor(get(urlPathEqualTo("/search/repositories"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(searchBody(150, false, repos(1, 100)))));

        stubFor(get(urlPathEqualTo("/search/repositories"))
                .withQueryParam("page", equalTo("2"))
                .willReturn(aResponse().withStatus(500)));

        GithubListingException exception = assertThrows(GithubListingException.class,
                () -> githubClient.listPortfolioRepos());

        assertInstanceOf(HttpStatusCodeException.class, exception.getCause());
    }

}
