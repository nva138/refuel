package at.maximilianmayer.veggietracker.whoop;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WhoopService {

    private static final String TOKEN_URL = "https://api.prod.whoop.com/oauth/oauth2/token";
    private static final String CYCLE_URL = "https://api.prod.whoop.com/developer/v2/cycle?limit=25";
    private static final double KJ_PER_KCAL = 4.184;
    private static final ZoneId VIENNA = ZoneId.of("Europe/Vienna");

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

        saveToken(requestToken(form));
    }

    public synchronized String getValidAccessToken() {
        WhoopToken token = whoopTokenRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new WhoopNotConnectedException());

        if (token.getExpiresAt().isAfter(Instant.now().plusSeconds(60))) {
            return token.getAccessToken();
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add("grant_type", "refresh_token");
        form.add("refresh_token", token.getRefreshToken());
        form.add("client_id", whoopProperties.clientId());
        form.add("client_secret", whoopProperties.clientSecret());
        form.add("scope", "offline");

        return saveToken(requestToken(form)).getAccessToken();
    }

    private WhoopTokenResponse requestToken(MultiValueMap<String, String> form) {
        return restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(WhoopTokenResponse.class);
    }

    private WhoopToken saveToken(WhoopTokenResponse response) {
        Instant expiresAt = Instant.now().plusSeconds(response.expiresIn());

        WhoopToken token = whoopTokenRepository.findAll().stream().findFirst().orElse(null);

        if (token == null) {
            token = new WhoopToken(response.accessToken(), response.refreshToken(), expiresAt);
        } else {
            token.update(response.accessToken(), response.refreshToken(), expiresAt);
        }

        return whoopTokenRepository.save(token);
    }

    public EnergyResponse getEnergy(LocalDate date){
        WhoopCycleResponse response = fetchCycles();

        LocalDate day = date != null ? date : LocalDate.now(VIENNA);
        double dayKj = 0;
        double weekKj = 0;
        int weekDays = 0;
        Instant weekStart = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant noon = day.atTime(12, 0).atZone(VIENNA).toInstant();

        for(WhoopCycle cycle : response.records()) {
            if (!"SCORED".equals(cycle.scoreState())) {
                continue;
            }
            if (containsNoon(cycle, noon)) {
                dayKj = cycle.score().kilojoule();
            }
            if (cycle.end() != null && cycle.start().isAfter(weekStart)) {
                weekKj += cycle.score().kilojoule();
                weekDays++;
            }
        }
        int dayKcal = (int) Math.round(dayKj / KJ_PER_KCAL);
        int weeklyAverageKcal = (int) Math.round(weekKj / weekDays / KJ_PER_KCAL);
        return new EnergyResponse(weeklyAverageKcal, dayKcal);
    }

    public Map<LocalDate, Integer> getBurnByDay(List<LocalDate> days) {
        WhoopCycleResponse response = fetchCycles();
        Map<LocalDate, Integer> burnByDay = new HashMap<>();

        for (LocalDate day : days) {
            Instant noon = day.atTime(12, 0).atZone(VIENNA).toInstant();
            for (WhoopCycle cycle : response.records()) {
                if ("SCORED".equals(cycle.scoreState()) && containsNoon(cycle, noon)) {
                    burnByDay.put(day, (int) Math.round(cycle.score().kilojoule() / KJ_PER_KCAL));
                }
            }
        }
        return burnByDay;
    }

    private WhoopCycleResponse fetchCycles() {
        return restClient.get()
                .uri(CYCLE_URL)
                .header("Authorization", "Bearer " + getValidAccessToken())
                .retrieve()
                .body(WhoopCycleResponse.class);
    }

    private boolean containsNoon(WhoopCycle cycle, Instant noon) {
        return cycle.start().isBefore(noon) && (cycle.end() == null || cycle.end().isAfter(noon));
    }
}
