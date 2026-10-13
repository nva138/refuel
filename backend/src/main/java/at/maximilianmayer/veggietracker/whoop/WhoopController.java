package at.maximilianmayer.veggietracker.whoop;


import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/whoop")
public class WhoopController {
    private final WhoopService whoopService;

    public WhoopController(WhoopService whoopService) {
        this.whoopService = whoopService;
    }

    @GetMapping("/login")
    public ResponseEntity<Void> login(HttpSession session) {
        String state = UUID.randomUUID().toString();
        session.setAttribute("whoopState", state);
        String url = whoopService.buildAuthorizationUrl(state);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url))
                .build();
    }

}
