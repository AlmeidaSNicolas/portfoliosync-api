package io.github.almeidasnicolas.portfoliosync.project.controller;

import io.github.almeidasnicolas.portfoliosync.project.dto.ProjectResponse;
import io.github.almeidasnicolas.portfoliosync.project.service.ProjectService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;

    @GetMapping
    public List<ProjectResponse> responseList(@RequestParam(defaultValue = "es") String lang){
        return projectService.listVisible(lang);
    }

}
