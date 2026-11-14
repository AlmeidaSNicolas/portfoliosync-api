package io.github.almeidasnicolas.portfoliosync.webhook.controller;

import io.github.almeidasnicolas.portfoliosync.github.exception.RepositoryNotFoundException;
import io.github.almeidasnicolas.portfoliosync.sync.service.SyncService;
import io.github.almeidasnicolas.portfoliosync.webhook.service.GithubPushPayloadParser;
import io.github.almeidasnicolas.portfoliosync.webhook.service.HmacValidator;
import io.github.almeidasnicolas.portfoliosync.webhook.service.WebhookDeliveryService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({HmacValidator.class, GithubPushPayloadParser.class})
@WebMvcTest(controllers = WebhookController.class, properties = "github.webhook.secret=It's a Secret to Everybody")
class WebhookControllerTest {

    private static final byte[] BODY = "Hello, World!".getBytes(StandardCharsets.UTF_8);
    private static final String HEADER = "sha256=757107ea0eb2509fc211221cce984b8a37570b6d7586c22c46f4379c8b043e17";
    private static final UUID DELIVERY_ID = UUID.fromString("72d3162e-cc78-11e3-81ab-4c9367dc0958");
    private static final byte[] PUSH_BODY = "{\"repository\":{\"id\":123,\"name\":\"meu-projeto\"}}".getBytes(StandardCharsets.UTF_8);
    private static final String PUSH_HEADER = "sha256=6831b1929ab97874ad41ab9a28cb91658cf32c51583663c03368b5069de25bb2";

    @MockitoBean
    private SyncService syncService;

    @MockitoBean
    private WebhookDeliveryService webhookDeliveryService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn200WhenSignatureIsValid() throws Exception{
        mockMvc.perform(post("/webhooks/github").header("X-GitHub-Event", "ping").header("X-Hub-Signature-256", HEADER).header("X-GitHub-Delivery", DELIVERY_ID).content(BODY)).andExpect(status().isOk());
    }

    @Test
    void alteredBody() throws Exception{
        mockMvc.perform(post("/webhooks/github").header("X-GitHub-Event", "ping").header("X-Hub-Signature-256", HEADER).header("X-GitHub-Delivery", DELIVERY_ID).content("Hello, World")).andExpect(status().isUnauthorized());
    }

    @Test
    void notPresentAsignature() throws Exception{
        mockMvc.perform(post("/webhooks/github").header("X-GitHub-Event", "ping").header("X-GitHub-Delivery", DELIVERY_ID).content(BODY)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn200AndRegisterOnceWhenDeliveryIsRepeated() throws Exception{
        when(webhookDeliveryService.registerIfNew(DELIVERY_ID, "ping")).thenReturn(false);

        mockMvc.perform(post("/webhooks/github").header("X-GitHub-Event", "ping").header("X-Hub-Signature-256", HEADER).header("X-GitHub-Delivery", DELIVERY_ID).content(BODY)).andExpect(status().isOk());

        verify(webhookDeliveryService, times(1)).registerIfNew(DELIVERY_ID, "ping");
    }

    @Test
    void shouldReturn200AndRegisterOnceWhenDeliveryIsNew() throws Exception{
        when(webhookDeliveryService.registerIfNew(DELIVERY_ID, "ping")).thenReturn(true);

        mockMvc.perform(post("/webhooks/github").header("X-GitHub-Event", "ping").header("X-Hub-Signature-256", HEADER).header("X-GitHub-Delivery", DELIVERY_ID).content(BODY)).andExpect(status().isOk());

        verify(webhookDeliveryService, times(1)).registerIfNew(DELIVERY_ID, "ping");
    }

    @Test
    void shouldMarkIgnoredWhenNewDeliveryIsPing() throws Exception{
        when(webhookDeliveryService.registerIfNew(DELIVERY_ID, "ping")).thenReturn(true);

        mockMvc.perform(post("/webhooks/github").header("X-GitHub-Event", "ping").header("X-Hub-Signature-256", HEADER).header("X-GitHub-Delivery", DELIVERY_ID).content(BODY)).andExpect(status().isOk());

        verify(webhookDeliveryService, times(1)).markIgnored(DELIVERY_ID);
    }

    @Test
    void shouldNotMarkIgnoredWhenDeliveryIsRepeated() throws Exception{
        when(webhookDeliveryService.registerIfNew(DELIVERY_ID, "ping")).thenReturn(false);

        mockMvc.perform(post("/webhooks/github").header("X-GitHub-Event", "ping").header("X-Hub-Signature-256", HEADER).header("X-GitHub-Delivery", DELIVERY_ID).content(BODY)).andExpect(status().isOk());

        verify(webhookDeliveryService, never()).markIgnored(DELIVERY_ID);
    }

    @Test
    void shouldMarkIgnoredAndSkipSyncWhenNewDeliveryIsAnUnknownEvent() throws Exception{
        // "issues" carries a valid push-shaped body on purpose: if the controller wrongly
        // took the push path, the parse would succeed and the sync would run.
        when(webhookDeliveryService.registerIfNew(DELIVERY_ID, "issues")).thenReturn(true);

        mockMvc.perform(post("/webhooks/github").header("X-GitHub-Event", "issues").header("X-Hub-Signature-256", PUSH_HEADER).header("X-GitHub-Delivery", DELIVERY_ID).content(PUSH_BODY)).andExpect(status().isOk());

        verify(webhookDeliveryService).markIgnored(DELIVERY_ID);
        verify(webhookDeliveryService, never()).recordRepository(any(), any(), any());
        verify(webhookDeliveryService, never()).markProcessed(any());
        verify(webhookDeliveryService, never()).markFailed(any(), any());
        verifyNoInteractions(syncService);
    }

    @Test
    void shouldRecordSyncAndMarkProcessedInOrderWhenPushIsNew() throws Exception{
        when(webhookDeliveryService.registerIfNew(DELIVERY_ID, "push")).thenReturn(true);

        mockMvc.perform(post("/webhooks/github").header("X-GitHub-Event", "push").header("X-Hub-Signature-256", PUSH_HEADER).header("X-GitHub-Delivery", DELIVERY_ID).content(PUSH_BODY)).andExpect(status().isOk());

        InOrder inOrder = inOrder(webhookDeliveryService, syncService);
        inOrder.verify(webhookDeliveryService).recordRepository(DELIVERY_ID, 123L, "meu-projeto");
        inOrder.verify(syncService).sync("meu-projeto");
        inOrder.verify(webhookDeliveryService).markProcessed(DELIVERY_ID);
        verify(webhookDeliveryService, never()).markFailed(any(), any());
    }

    @Test
    void shouldMarkFailedAndRethrowWhenSyncThrows() throws Exception{
        when(webhookDeliveryService.registerIfNew(DELIVERY_ID, "push")).thenReturn(true);
        doThrow(new RepositoryNotFoundException("repo not found", null)).when(syncService).sync("meu-projeto");

        ServletException thrown = assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/webhooks/github").header("X-GitHub-Event", "push").header("X-Hub-Signature-256", PUSH_HEADER).header("X-GitHub-Delivery", DELIVERY_ID).content(PUSH_BODY)));

        assertInstanceOf(RepositoryNotFoundException.class, thrown.getCause());
        verify(webhookDeliveryService).markFailed(DELIVERY_ID, "repo not found");
        verify(webhookDeliveryService, never()).markProcessed(any());
    }

    @Test
    void shouldMarkFailedAndRethrowWhenSyncThrowsAnyOtherRuntimeException() throws Exception{
        when(webhookDeliveryService.registerIfNew(DELIVERY_ID, "push")).thenReturn(true);
        doThrow(new IllegalStateException("unexpected failure")).when(syncService).sync("meu-projeto");

        ServletException thrown = assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/webhooks/github").header("X-GitHub-Event", "push").header("X-Hub-Signature-256", PUSH_HEADER).header("X-GitHub-Delivery", DELIVERY_ID).content(PUSH_BODY)));

        assertInstanceOf(IllegalStateException.class, thrown.getCause());
        verify(webhookDeliveryService).markFailed(DELIVERY_ID, "unexpected failure");
        verify(webhookDeliveryService, never()).markProcessed(any());
    }



}
