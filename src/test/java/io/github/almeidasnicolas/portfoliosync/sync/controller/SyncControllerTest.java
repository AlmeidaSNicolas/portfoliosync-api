package io.github.almeidasnicolas.portfoliosync.sync.controller;

import io.github.almeidasnicolas.portfoliosync.github.exception.GithubListingException;
import io.github.almeidasnicolas.portfoliosync.shared.security.SecurityConfig;
import io.github.almeidasnicolas.portfoliosync.sync.dto.SyncSummary;
import io.github.almeidasnicolas.portfoliosync.sync.service.SyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(controllers = SyncController.class, properties = {"github.webhook.secret=qualquerTeste", "admin.token.secret=qualquerTeste", "github.token=qualquerTeste"})
class SyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SyncService syncService;

    @Test
    void shouldReturn401WhenTokenIsMissing() throws Exception {
        mockMvc.perform(post("/admin/sync"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(syncService);
    }

    @Test
    void shouldReturn401WhenTokenIsWrong() throws Exception {
        mockMvc.perform(post("/admin/sync").header("X-Admin-Token", "errado"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(syncService);
    }

    @Test
    void shouldReturn200WithSummaryWhenTokenIsValid() throws Exception {
        when(syncService.resyncAll()).thenReturn(new SyncSummary(2, 1, 1, List.of()));

        mockMvc.perform(post("/admin/sync").header("X-Admin-Token", "qualquerTeste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.succeeded").value(1))
                .andExpect(jsonPath("$.failed").value(1));
    }

    @Test
    void shouldReturn502WhenGithubListingFails() throws Exception {
        when(syncService.resyncAll()).thenThrow(new GithubListingException("github listing failed"));

        mockMvc.perform(post("/admin/sync").header("X-Admin-Token", "qualquerTeste"))
                .andExpect(status().isBadGateway());
    }
}
