package io.github.almeidasnicolas.portfoliosync.project.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long githubRepoId;
    private String repoName;
    private String htmlUrl;
    @JdbcTypeCode(SqlTypes.JSON)
    private LocalizedText title;
    @JdbcTypeCode(SqlTypes.JSON)
    private LocalizedText description;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> stack = new ArrayList<>();
    private String demoUrl;
    private Boolean featured;
    private Integer displayOrder;
    private boolean visible = true;
    private Instant pushedAt;
    private Instant syncedAt;
    private String lastError;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    protected Project() {

    }

    public Project(Long githubRepoId, String repoName, String htmlUrl, LocalizedText title, LocalizedText description, List<String> stack, String demoUrl, boolean featured, Integer displayOrder, Instant pushedAt) {
        this.githubRepoId = githubRepoId;
        this.repoName = repoName;
        this.htmlUrl = htmlUrl;
        this.title = title;
        this.description = description;
        this.stack = stack != null ? stack : new ArrayList<>();
        this.demoUrl = demoUrl;
        this.featured = featured;
        this.displayOrder = displayOrder;
        this.pushedAt = pushedAt;
        this.syncedAt = Instant.now();
    }

}
