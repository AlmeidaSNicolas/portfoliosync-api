package io.github.almeidasnicolas.portfoliosync.project.repository;

import io.github.almeidasnicolas.portfoliosync.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByVisibleTrue();

    Optional<Project> findByGithubRepoId(Long githubRepoId);
}
