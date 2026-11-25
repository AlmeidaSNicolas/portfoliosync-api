package io.github.almeidasnicolas.portfoliosync.sync.controller;

import io.github.almeidasnicolas.portfoliosync.sync.dto.SyncSummary;
import io.github.almeidasnicolas.portfoliosync.sync.service.SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/sync")
public class SyncController {

    private final SyncService syncService;

    @PostMapping
    public SyncSummary resync(){
        return syncService.resyncAll();
    }

}
