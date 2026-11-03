package io.github.almeidasnicolas.portfoliosync.github;
import org.springframework.web.client.RestClient;

public class GithubClient {

    public static void main(String[] args) {
        RestClient customClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .build();

        String result = customClient.get()
                .uri("/repos/AlmeidaSNicolas/{repo}/contents/portfolio.json", "30-dias-java")
                .retrieve()
                .body(String.class);

        System.out.println(result);
    }
}
