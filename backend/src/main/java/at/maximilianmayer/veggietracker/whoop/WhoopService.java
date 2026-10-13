package at.maximilianmayer.veggietracker.whoop;

import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class WhoopService {
    private final WhoopTokenRepository whoopTokenRepository;
    private final WhoopProperties whoopProperties;

    public WhoopService(WhoopTokenRepository whoopTokenRepository, WhoopProperties whoopProperties) {
        this.whoopTokenRepository = whoopTokenRepository;
        this.whoopProperties = whoopProperties;
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
}
