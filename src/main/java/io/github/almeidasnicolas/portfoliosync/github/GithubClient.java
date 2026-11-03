package io.github.almeidasnicolas.portfoliosync.github;
import org.springframework.stereotype.Component;

import org.springframework.web.client.RestClient;

@Component
public class GithubClient {

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.github.com")
            .build();

    public String getPortfolioFile(String repoName){
             return restClient.get()
                .uri("/repos/AlmeidaSNicolas/{repo}/contents/portfolio.json", repoName)
                .retrieve()
                .body(String.class);
    }
}
