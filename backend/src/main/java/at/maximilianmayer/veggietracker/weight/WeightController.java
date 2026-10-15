package at.maximilianmayer.veggietracker.weight;


import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/weight")
public class WeightController {
    private final WeightService weightService;

    public WeightController(WeightService weightService) {
        this.weightService = weightService;

    }

    @PostMapping
    public WeightEntryResponse logWeight(@RequestBody LogWeightRequest logWeightRequest) {
        return weightService.logWeight(logWeightRequest);
    }

    @GetMapping
    public List<WeightEntryResponse> getHistory() {
        return weightService.getHistory();
    }
}
