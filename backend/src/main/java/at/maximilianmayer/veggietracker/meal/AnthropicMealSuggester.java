package at.maximilianmayer.veggietracker.meal;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.*;
import org.springframework.stereotype.Component;

@Component
public class AnthropicMealSuggester implements MealSuggester {

    private static final String SYSTEM_PROMPT = """
            You help a user log meals in a nutrition tracker. The user describes what they ate in a
            short text, usually in German. Turn it into a structured suggestion that the user will
            review and correct before saving.

            Name
            - Give the meal a short, natural name in the language the user wrote in, for example
              "Rührei mit Vollkornbrot". Keep it under 40 characters.

            Parts
            - Split the meal into its separate components, one part per food. A sandwich becomes
              bread, spread and topping. A simple single food stays one part.
            - Name each part in the user's language.
            - Give kcal, protein, carbs and fat per 100 g of that part as eaten (cooked rice, not
              dry rice). Use typical values from standard nutrition tables.
            - Estimate amountG, the eaten amount in grams. Use the quantities the user gives
              ("2 eggs", "a plate", "200 g"). If no amount is given, assume one typical adult
              portion. Convert pieces and household measures into grams (one egg is about 55 g,
              one slice of wholegrain bread about 45 g).
            - Include fats and sauces that are almost always part of the dish even if not
              mentioned, such as oil for fried food or butter on a sandwich, as their own part
              with a realistic amount.
            - Never ask questions. If something is unclear, make the most likely assumption.

            Veggie hints
            - The user eats vegetarian. List short warnings, in the user's language, only for
              ingredients that might not be vegetarian, for example meat or chicken stock, fish
              sauce, gelatin, animal rennet in cheese, or lard.
            - Never claim that a meal is vegetarian. If nothing is suspicious, return an empty list.
            - Do not warn about eggs, dairy or honey.
            """;

    private final AnthropicClient client = AnthropicOkHttpClient.fromEnv();

    @Override
    public SuggestMealResponse suggest(String text) {
        StructuredMessageCreateParams<SuggestMealResponse> params = MessageCreateParams.builder()
                .model(Model.CLAUDE_HAIKU_5_5)
                .maxTokens(2048)
                .system(SYSTEM_PROMPT)
                .addUserMessage(text)
                .outputConfig(SuggestMealResponse.class)
                .build();

        StructuredMessage<SuggestMealResponse> message = client.messages().create(params);

        return message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(StructuredTextBlock::text)
                .findFirst()
                .orElseThrow();
    }
}