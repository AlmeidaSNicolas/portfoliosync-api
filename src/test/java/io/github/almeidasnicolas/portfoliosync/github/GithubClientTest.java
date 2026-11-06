package io.github.almeidasnicolas.portfoliosync.github;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

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

}
