package io.github.almeidasnicolas.portfoliosync.github;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@Component
public class GithubClient {

    private final JsonMapper jsonMapper;
    private final RestClient restClient;


    public GithubClient(JsonMapper jsonMapper, @Value("${github.api.base-url}") String url) {
        this.jsonMapper = jsonMapper;
        this.restClient = RestClient.builder().baseUrl(url).build();
    }

    public PortfolioFile getPortfolioFile(String repoName){
        try{
            String jsonResult = restClient.get()
                    .uri("/repos/AlmeidaSNicolas/{repo}/contents/portfolio.json", repoName)
                    .retrieve()
                    .body(ContentDTO.class).decode();

            return jsonMapper.readValue(jsonResult, PortfolioFile.class);

        }catch (HttpClientErrorException.NotFound e){
            throw new PortfolioFileNotFoundException("portfolio.json not found in repo: " + repoName, e );
        }


    }
}
