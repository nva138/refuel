package at.maximilianmayer.veggietracker.weight;

import at.maximilianmayer.veggietracker.settings.Settings;
import at.maximilianmayer.veggietracker.settings.SettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class WeightService {
    private static final ZoneId VIENNA = ZoneId.of("Europe/Vienna");

    private final WeightEntryRepository weightEntryRepository;
    private final SettingsRepository settingsRepository;

    public WeightService(WeightEntryRepository weightEntryRepository, SettingsRepository settingsRepository) {
        this.weightEntryRepository = weightEntryRepository;
        this.settingsRepository = settingsRepository;
    }

    @Transactional
    public WeightEntryResponse logWeight(LogWeightRequest request) {
        LocalDate today = LocalDate.now(VIENNA);
        WeightEntry entry = weightEntryRepository.findByMeasuredOn(today).orElse(null);

        if(entry == null) {
            entry = new WeightEntry(today, request.weightKg());
        } else {
            entry.setWeightKg(request.weightKg());
        }

        WeightEntry saved = weightEntryRepository.save(entry);

        Settings settings = settingsRepository.findAll().getFirst();
        settings.setBodyWeightKg(request.weightKg());
        settingsRepository.save(settings);

        return WeightEntryResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<WeightEntryResponse> getHistory() {
        LocalDate weightRecordingStart = LocalDate.now(VIENNA).minusDays(90);
        return weightEntryRepository.findByMeasuredOnGreaterThanEqualOrderByMeasuredOnAsc(weightRecordingStart)
                .stream()
                .map(WeightEntryResponse::from)
                .toList();
    }
}
