package at.maximilianmayer.veggietracker.whoop;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class WhoopNotConnectedException extends RuntimeException {
    public WhoopNotConnectedException() {
        super("WHOOP is not connected");
    }
}
