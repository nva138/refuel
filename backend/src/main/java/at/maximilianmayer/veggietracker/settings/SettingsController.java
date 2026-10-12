package at.maximilianmayer.veggietracker.settings;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public SettingsResponse getSettings() {
        return settingsService.getSettings();
    }

    @PutMapping
    public SettingsResponse saveSettings(@RequestBody UpdateSettingsRequest request) {
        return settingsService.updateSettings(request);
    }
}
