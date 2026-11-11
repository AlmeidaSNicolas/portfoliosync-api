package io.github.almeidasnicolas.portfoliosync.project.service;

import io.github.almeidasnicolas.portfoliosync.github.dto.PortfolioFile;
import io.github.almeidasnicolas.portfoliosync.github.dto.RepoMetadata;
import io.github.almeidasnicolas.portfoliosync.project.domain.Project;
import io.github.almeidasnicolas.portfoliosync.project.dto.ProjectResponse;
import io.github.almeidasnicolas.portfoliosync.project.repository.ProjectRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class ProjectService {
    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public List<ProjectResponse> listVisible(String lang){
        List<Project> projects = projectRepository.findByVisibleTrue();


        return projects.stream().map(project -> new ProjectResponse(project.getRepoName()
                ,project.getTitle().in(lang),
                project.getDescription().in(lang),
                project.getStack(),
                project.getHtmlUrl(),
                project.getDemoUrl(),
                project.isFeatured() )
                ).toList();
    }

    @Transactional
    public void saveOrUpdate(RepoMetadata repoMetadata, PortfolioFile portfolioFile){
        Long searchId = repoMetadata.id();
        Optional<Project> existing = projectRepository.findByGithubRepoId(searchId);

        boolean featured = Boolean.TRUE.equals(portfolioFile.featured());


        if (existing.isPresent()){
            Project found = existing.get();
            found.updateFromGithub(portfolioFile.title(),
                    portfolioFile.description(),
                    portfolioFile.stack(),
                    portfolioFile.demoUrl(),
                    featured,
                    portfolioFile.order(),
                    repoMetadata.pushedAt(),
                    repoMetadata.name(),
                    repoMetadata.htmlUrl()
                    );
        }else{
            Project project = new Project(repoMetadata.id(),
                    repoMetadata.name(),
                    repoMetadata.htmlUrl(),
                    portfolioFile.title(),
                    portfolioFile.description(),
                    portfolioFile.stack(),
                    portfolioFile.demoUrl(),
                    featured,
                    portfolioFile.order(),
                    repoMetadata.pushedAt()
                    );
            projectRepository.save(project);

        }

    }

}
