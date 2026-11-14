package io.github.almeidasnicolas.portfoliosync.sync.service;

import io.github.almeidasnicolas.portfoliosync.github.client.GithubClient;
import io.github.almeidasnicolas.portfoliosync.github.dto.PortfolioFile;
import io.github.almeidasnicolas.portfoliosync.github.dto.RepoMetadata;
import io.github.almeidasnicolas.portfoliosync.github.exception.InvalidPortfolioFileException;
import io.github.almeidasnicolas.portfoliosync.github.exception.PortfolioFileNotFoundException;
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
            projectService.hide(repoMetadata.id());
            return;
        }

        PortfolioFile portfolioFile;
        try{
            portfolioFile = githubClient.getPortfolioFile(repoName);
            portfolioValidator.validate(portfolioFile);
        }catch (PortfolioFileNotFoundException | InvalidPortfolioFileException e){
            projectService.hide(repoMetadata.id());
            projectService.registerError( repoMetadata.id() ,e.getMessage());
            throw e;
        }
        projectService.saveOrUpdate(repoMetadata, portfolioFile);

    }


}
