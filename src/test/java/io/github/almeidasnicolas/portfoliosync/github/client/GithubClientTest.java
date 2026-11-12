package io.github.almeidasnicolas.portfoliosync.github.client;

import io.github.almeidasnicolas.portfoliosync.github.dto.PortfolioFile;
import io.github.almeidasnicolas.portfoliosync.github.dto.RepoMetadata;
import io.github.almeidasnicolas.portfoliosync.github.exception.PortfolioFileNotFoundException;
import io.github.almeidasnicolas.portfoliosync.github.exception.RepositoryNotFoundException;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


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

}
