package at.maximilianmayer.veggietracker.whoop;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;

@Service
public class WhoopService {

    private static final String TOKEN_URL = "https://api.prod.whoop.com/oauth/oauth2/token";

    private final WhoopTokenRepository whoopTokenRepository;
    private final WhoopProperties whoopProperties;
    private final RestClient restClient;

    public WhoopService(WhoopTokenRepository whoopTokenRepository, WhoopProperties whoopProperties, RestClient.Builder restClientBuilder) {
        this.whoopTokenRepository = whoopTokenRepository;
        this.whoopProperties = whoopProperties;
        this.restClient = restClientBuilder.build();
    }

    public String buildAuthorizationUrl(String state) {
        return UriComponentsBuilder.fromUriString("https://api.prod.whoop.com/oauth/oauth2/auth")
                .queryParam("response_type", "code")
                .queryParam("client_id", whoopProperties.clientId())
                .queryParam("redirect_uri", whoopProperties.redirectUri())
                .queryParam("scope", "read:cycles offline")
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
    }

    public void exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add("grant_type", "authorization_code");
        form.add("redirect_uri", whoopProperties.redirectUri());
        form.add("client_id", whoopProperties.clientId());
        form.add("client_secret", whoopProperties.clientSecret());
        form.add("code", code);

        WhoopTokenResponse response = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(WhoopTokenResponse.class);

        Instant expiresAt = Instant.now().plusSeconds(response.expiresIn());

        WhoopToken token = whoopTokenRepository.findAll().stream().findFirst().orElse(null);

        if(token == null) {
        token = new WhoopToken(response.accessToken(), response.refreshToken(), expiresAt);
        } else {
            token.update(response.accessToken(), response.refreshToken(), expiresAt);
        }

        whoopTokenRepository.save(token);
    }
}
