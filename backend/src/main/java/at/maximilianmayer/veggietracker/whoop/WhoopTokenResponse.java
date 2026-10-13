package at.maximilianmayer.veggietracker.whoop;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WhoopTokenResponse(@JsonProperty("access_token") String accessToken,
                                 @JsonProperty("refresh_token") String refreshToken,
                                 @JsonProperty("expires_in") Integer expiresIn) {
}
