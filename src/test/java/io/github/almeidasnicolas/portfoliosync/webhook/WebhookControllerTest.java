package io.github.almeidasnicolas.portfoliosync.webhook;

import io.github.almeidasnicolas.portfoliosync.webhook.controller.WebhookController;
import io.github.almeidasnicolas.portfoliosync.webhook.service.HmacValidator;
import io.github.almeidasnicolas.portfoliosync.webhook.service.WebhookDeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({HmacValidator.class})
@WebMvcTest(controllers = WebhookController.class, properties = "github.webhook.secret=It's a Secret to Everybody")
public class WebhookControllerTest {

    private static final byte[] BODY = "Hello, World!".getBytes(StandardCharsets.UTF_8);
    private static final String HEADER = "sha256=757107ea0eb2509fc211221cce984b8a37570b6d7586c22c46f4379c8b043e17";
    private static final UUID DELIVERY_ID = UUID.fromString("72d3162e-cc78-11e3-81ab-4c9367dc0958");

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
}
