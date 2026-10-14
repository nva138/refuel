package at.maximilianmayer.veggietracker.whoop;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record WhoopCycle(Instant end, @JsonProperty("score_state") String scoreState, WhoopScore score) {
}
