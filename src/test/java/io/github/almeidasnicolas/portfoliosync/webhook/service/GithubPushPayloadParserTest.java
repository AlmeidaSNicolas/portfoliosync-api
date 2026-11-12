package io.github.almeidasnicolas.portfoliosync.webhook.service;

import io.github.almeidasnicolas.portfoliosync.webhook.dto.GithubPushBody;
import io.github.almeidasnicolas.portfoliosync.webhook.exception.InvalidWebhookPayloadFormatException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GithubPushPayloadParserTest {

    private final GithubPushPayloadParser parser = new GithubPushPayloadParser(JsonMapper.builder().build());

    private static byte[] bytes(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void shouldReadRepositoryIdAndName() {
        GithubPushBody body = parser.parse(bytes("""
                {"repository": {"id": 123, "name": "portfoliosync-api"}}
                """));

        assertEquals(123L, body.repository().id());
        assertEquals("portfoliosync-api", body.repository().name());
    }

    @Test
    void shouldIgnoreFieldsItDoesNotKnow() {
        GithubPushBody body = parser.parse(bytes("""
                {"ref": "refs/heads/main",
                 "commits": [],
                 "repository": {"id": 123, "name": "portfoliosync-api", "private": false}}
                """));

        assertEquals("portfoliosync-api", body.repository().name());
    }

    @Test
    void shouldRejectPayloadWithoutRepository() {
        assertThrows(InvalidWebhookPayloadFormatException.class,
                () -> parser.parse(bytes("{}")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"repository\": {\"id\": 123}}",
            "{\"repository\": {\"id\": 123, \"name\": null}}",
            "{\"repository\": {\"id\": 123, \"name\": \"\"}}",
            "{\"repository\": {\"id\": 123, \"name\": \"   \"}}"
    })
    void shouldRejectMissingNullOrBlankRepositoryName(String json) {
        assertThrows(InvalidWebhookPayloadFormatException.class,
                () -> parser.parse(bytes(json)));
    }

    @Test
    void shouldLetJacksonExceptionEscapeOnBrokenJson() {
        assertThrows(JacksonException.class,
                () -> parser.parse(bytes("{not json")));
    }
}
