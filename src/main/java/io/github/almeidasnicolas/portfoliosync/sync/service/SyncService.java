package io.github.almeidasnicolas.portfoliosync.sync.service;

import io.github.almeidasnicolas.portfoliosync.github.client.GithubClient;
import io.github.almeidasnicolas.portfoliosync.github.dto.PortfolioFile;
import io.github.almeidasnicolas.portfoliosync.github.dto.RepoMetadata;
import io.github.almeidasnicolas.portfoliosync.github.service.PortfolioValidator;
import io.github.almeidasnicolas.portfoliosync.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class SyncService {
    private final GithubClient githubClient;
    private final PortfolioValidator portfolioValidator;
    private final ProjectService projectService;

    public void sync(String repoName){
        RepoMetadata repoMetadata = githubClient.getRepoMetadata(repoName);

        if (!repoMetadata.topics().contains("portfolio")){
        return;
        }
        PortfolioFile portfolioFile = githubClient.getPortfolioFile(repoName);
        portfolioValidator.validate(portfolioFile);
        projectService.saveOrUpdate(repoMetadata, portfolioFile);
    }


}
