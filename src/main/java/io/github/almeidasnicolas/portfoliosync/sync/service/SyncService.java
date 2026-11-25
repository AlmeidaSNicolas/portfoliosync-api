package io.github.almeidasnicolas.portfoliosync.sync.service;

import io.github.almeidasnicolas.portfoliosync.github.client.GithubClient;
import io.github.almeidasnicolas.portfoliosync.github.dto.PortfolioFile;
import io.github.almeidasnicolas.portfoliosync.github.dto.RepoMetadata;
import io.github.almeidasnicolas.portfoliosync.github.exception.InvalidPortfolioFileException;
import io.github.almeidasnicolas.portfoliosync.github.exception.PortfolioFileNotFoundException;
import io.github.almeidasnicolas.portfoliosync.github.service.PortfolioValidator;
import io.github.almeidasnicolas.portfoliosync.project.service.ProjectService;
import io.github.almeidasnicolas.portfoliosync.sync.dto.SyncFailure;
import io.github.almeidasnicolas.portfoliosync.sync.dto.SyncSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class SyncService {
    private final GithubClient githubClient;
    private final PortfolioValidator portfolioValidator;
    private final ProjectService projectService;

    public void sync(String repoName){
        RepoMetadata repoMetadata = githubClient.getRepoMetadata(repoName);

        repoRecive(repoMetadata);

    }

    public SyncSummary syncAll(List<RepoMetadata> repos){
        int succeeded = 0;
        List<SyncFailure> failures = new ArrayList<>();

        for (RepoMetadata repo : repos){
            try{
                repoRecive(repo);
                succeeded++;
            }catch (RuntimeException e){
                failures.add(new SyncFailure(repo.name(), e.getMessage()));
            }
        }

        return new SyncSummary(repos.size(), succeeded, failures.size(), failures);
    }

    private void repoRecive(RepoMetadata repoMetadata){
        if (!repoMetadata.topics().contains("portfolio")){
            projectService.hide(repoMetadata.id());
            return;
        }

        PortfolioFile portfolioFile;
        try{
            portfolioFile = githubClient.getPortfolioFile(repoMetadata.name());
            portfolioValidator.validate(portfolioFile);
        }catch (PortfolioFileNotFoundException | InvalidPortfolioFileException e){
            projectService.hide(repoMetadata.id());
            projectService.registerError( repoMetadata.id() ,e.getMessage());
            throw e;
        }
        projectService.saveOrUpdate(repoMetadata, portfolioFile);
    }
}
