package at.maximilianmayer.veggietracker.settings;

import org.springframework.stereotype.Service;

@Service
public class SettingsService {
    private final SettingsRepository settingsRepository;

    public SettingsService (SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    public SettingsResponse getSettings() {
        Settings setting = settingsRepository.findAll().getFirst();
        return SettingsResponse.from(setting);
    }

    public SettingsResponse updateSettings(UpdateSettingsRequest request) {
        Settings setting = settingsRepository.findAll().getFirst();
        setting.setMode(request.mode());
        setting.setBodyWeightKg(request.bodyWeightKg());
        setting.setKcalReference(request.kcalReference());
        Settings saved = settingsRepository.save(setting);
        return SettingsResponse.from(saved);
    }
}
