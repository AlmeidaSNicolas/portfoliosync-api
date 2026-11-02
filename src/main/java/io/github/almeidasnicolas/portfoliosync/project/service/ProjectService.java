package io.github.almeidasnicolas.portfoliosync.project.service;

import io.github.almeidasnicolas.portfoliosync.project.domain.Project;
import io.github.almeidasnicolas.portfoliosync.project.dto.ProjectResponse;
import io.github.almeidasnicolas.portfoliosync.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;


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


}
