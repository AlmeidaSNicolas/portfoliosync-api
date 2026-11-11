package io.github.almeidasnicolas.portfoliosync.webhook.service;

import io.github.almeidasnicolas.portfoliosync.webhook.dto.GithubPushBody;
import io.github.almeidasnicolas.portfoliosync.webhook.dto.GithubPushRepository;
import io.github.almeidasnicolas.portfoliosync.webhook.exception.InvalidWebhookPayloadFormatException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@RequiredArgsConstructor
@Service
public class ExtractDataFromGithubPush {

    private final JsonMapper jsonMapper;

    public GithubPushBody dataRetur(byte[] body){
        GithubPushBody githubPushBody = jsonMapper.readValue(body, GithubPushBody.class);

        GithubPushRepository repository = githubPushBody.repository();

        if (repository == null || repository.name() == null || repository.name().isBlank()){
            throw new InvalidWebhookPayloadFormatException("repository.name is required in the push payload");
        }

        return githubPushBody;

    }

}
