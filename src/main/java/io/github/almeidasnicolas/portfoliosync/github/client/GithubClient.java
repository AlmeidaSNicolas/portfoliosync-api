package io.github.almeidasnicolas.portfoliosync.github.client;

import io.github.almeidasnicolas.portfoliosync.github.dto.ContentDTO;
import io.github.almeidasnicolas.portfoliosync.github.dto.PortfolioFile;
import io.github.almeidasnicolas.portfoliosync.github.dto.RepoMetadata;
import io.github.almeidasnicolas.portfoliosync.github.dto.SearchReposResponse;
import io.github.almeidasnicolas.portfoliosync.github.exception.GithubListingException;
import io.github.almeidasnicolas.portfoliosync.github.exception.PortfolioFileNotFoundException;
import io.github.almeidasnicolas.portfoliosync.github.exception.RepositoryNotFoundException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

@Component
public class GithubClient {

    private final JsonMapper jsonMapper;
    private final RestClient restClient;
    private static final int PAGE_SIZE = 100;

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

    public RepoMetadata getRepoMetadata(String repoName){
        try{
            RepoMetadata repoMetadata = restClient.get()
                    .uri("/repos/AlmeidaSNicolas/{repo}", repoName)
                    .retrieve()
                    .body(RepoMetadata.class);

            return repoMetadata;

        }catch (HttpClientErrorException.NotFound e){
            throw new RepositoryNotFoundException("repository not found: " + repoName, e );
        }
    }

    public List<RepoMetadata> listPortfolioRepos() {
        List<RepoMetadata> actualRepos = new ArrayList<>();
        int page = 1;


        while (true) {

            SearchReposResponse response;

            try {
                response = restClient.get()
                        .uri("/search/repositories?q={q}&per_page={perPage}&page={page}",
                                "user:AlmeidaSNicolas topic:portfolio", PAGE_SIZE, page)
                        .retrieve()
                        .body(SearchReposResponse.class);
            } catch (HttpStatusCodeException e) {
                throw new GithubListingException("failed to list portfolio repos on page " + page + " with status " + e.getStatusCode(), e);
            }

            if (response.incompleteResults()) {
                throw new GithubListingException("search results incomplete");
            }

            List<RepoMetadata> items = response.items();
            if (items.isEmpty()) {
                if (actualRepos.size() < response.totalCount()) {
                    throw new GithubListingException("empty page before reaching total");
                }
                break;
            }

            actualRepos.addAll(items);

            if (actualRepos.size() >= response.totalCount()) {
                break;
            }
            page++;
        }

        return actualRepos;
    }

}
