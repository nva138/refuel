package at.maximilianmayer.veggietracker.whoop;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "whoop")
public record WhoopProperties(String clientId,
                              String clientSecret,
                              String redirectUri) {
}
