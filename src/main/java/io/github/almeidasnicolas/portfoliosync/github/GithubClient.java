package io.github.almeidasnicolas.portfoliosync.github;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@Component
public class GithubClient {

    private final JsonMapper jsonMapper;

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.github.com")
            .build();

    public GithubClient(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public PortfolioFile getPortfolioFile(String repoName){
        String jsonResult = restClient.get()
                .uri("/repos/AlmeidaSNicolas/{repo}/contents/portfolio.json", repoName)
                .retrieve()
                .body(ContentDTO.class).decode();

        return jsonMapper.readValue(jsonResult, PortfolioFile.class);
    }
}
