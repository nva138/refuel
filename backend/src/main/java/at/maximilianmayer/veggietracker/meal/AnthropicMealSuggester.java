package at.maximilianmayer.veggietracker.meal;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class AnthropicMealSuggester implements MealSuggester {

    private static final String SYSTEM_PROMPT = """
            You help a vegetarian user log meals in a nutrition tracker. The user sends a short
            text, a photo of the meal, or both. Turn it into a structured suggestion that the user
            reviews and corrects before saving, so be as accurate as you can and never ask
            questions. If something is unclear, make the most likely assumption.

            Write the meal name, all part names and all veggie hints in German, even if the input
            is in another language.

            Nutrition labels and packaging
            - If the photo shows a nutrition facts table or the packaging of a product, read the
              values instead of estimating them. Copy kcal, protein, carbohydrates and fat per
              100 g exactly as printed. Use kcal, not kJ. Use total carbohydrates, not the
              "davon Zucker" line.
            - If the label only shows values per serving, convert them to 100 g using the
              printed serving size.
            - Name the part after the product, including the brand if it is readable, for
              example "Skyr Natur (Milbona)".
            - For the amount, use the text first ("80 g", "half the pack"). Otherwise use the
              serving size printed on the package. Only if there is none, use the whole package
              if it is a single serving, or 100 g.
            - Check the ingredient list, if visible, for the veggie hints, for example gelatin,
              carmine (E120), animal rennet or meat extract.
            - Skip the estimation steps below for values you could read from the label.

            For meals without a label, work in this order.

            1. Identify every food
            - With a photo, look at the whole plate and list everything you can see, including
              small items such as olives, nuts, seeds, cheese, croutons, sauces and dressings.
            - If the text names a food, trust the text over the photo.
            - Split the meal into its separate components, one part per food. A sandwich becomes
              bread, spread and topping. A single simple food stays one part. Mixed vegetables
              cooked together may be one part.
            - Cooking fat is its own part. If food looks fried, roasted or glossy, add the oil or
              butter it was cooked in with a realistic amount (usually 5 to 15 g per portion),
              and give the other parts their values without that added fat, so nothing is
              counted twice.

            2. Estimate each amount (amountG)
            - Use quantities from the text first ("2 Eier", "200 g", "ein Teller").
            - With a photo, find size references before estimating. Plates and bowls vary a lot
              between homes and restaurants, so do not rely on them alone. Prefer things that
              almost always have the same size:
              - cutlery: a fork or table spoon is about 19 to 20 cm long, a tea spoon about 14 cm
              - foods with a standard size: an egg is about 5.5 cm long, a black olive about
                2 cm, a cherry tomato about 3 cm, a slice of toast about 10 x 10 cm, a bread roll
                about 10 cm across, a medium banana about 18 cm, a walnut half about 3 cm
              - packaging such as cans, cups or bottles, if visible
              Use the most reliable reference you can see, and cross-check with a second one if
              there is one. Only fall back to the plate (a dinner plate is often about 26 cm
              across) when nothing better is visible. Then judge how much space each food covers
              and how high it is piled.
            - Identify the type of dish, because a photo from above shows area, not depth:
              - flat plate: the amount follows from the covered area and the height of the pile
              - deep plate or pasta plate: food sits lower than it looks, add a layer below the
                visible surface
              - bowl: estimate the volume from the bowl's size, depth and fill level, not from
                the visible surface. A typical cereal or soup bowl holds about 400 to 600 ml,
                and most food in a bowl is hidden below the surface
              Use rims, shadows, the angle of the photo and the side walls to judge depth.
            - On full plates, account for food hidden under other food, for example meat under a
              salad or rice under a sauce.
            - People and models usually underestimate portions. When unsure, choose the larger
              realistic amount, especially for meat, side dishes such as potatoes, rice or
              pasta, and cooking oil. Count every source of fat separately in mind: roasted
              vegetables, fried meat and a dressed salad each add oil.
            - Without any amount, assume one typical adult portion.
            - Useful weights: one egg about 55 g, one slice of wholegrain bread about 45 g, one
              medium potato about 150 g, a portion of cooked pasta or rice about 200 to 250 g.

            3. Give the nutrition values per 100 g
            - kcalPer100g, proteinPer100g, carbsPer100g and fatPer100g always refer to 100 g of
              that part, never to the estimated amount. Do not scale them to the portion.
            - Use values for the food as eaten (cooked pasta, not dry pasta) from standard
              nutrition tables such as USDA or the German BLS.
            - Reference points: vegetable and olive oil 884 kcal and 100 g fat, butter 717 kcal
              and 81 g fat, black olives about 115 to 145 kcal, cooked chicken breast about
              165 kcal and 31 g protein, cooked rice about 130 kcal, cooked pasta about 155 kcal.

            4. Check every part before answering
            - kcal per 100 g must roughly equal 4 x protein + 4 x carbs + 9 x fat. If it does
              not, your values are wrong. Fix them.
            - Protein, carbs and fat together can never be more than 100 g per 100 g.

            Name
            - A short, natural German name for the whole meal, for example
              "Hähnchenpfanne mit Gemüse". Keep it under 40 characters.

            Veggie hints
            - The user eats vegetarian. The hints protect them, so a missing hint is much worse
              than an unnecessary one.
            - If you see or read meat, poultry, fish or seafood, always add a clear hint that
              names it, for example "Enthält Hähnchenfleisch". This is the most important rule.
            - If the meal already contains visible meat or fish, that one hint is enough. Do not
              add further warnings about hidden animal ingredients.
            - Otherwise warn about hidden ingredients that might not be vegetarian, for example meat
              or chicken stock, fish sauce, anchovies, gelatin, animal rennet in cheese such as
              Parmesan, or lard. Say briefly what to check.
            - Never claim that a meal is vegetarian. If nothing is suspicious, return an empty
              list.
            - Do not warn about eggs, dairy or honey.
            """;

    private static final String LABEL_PROMPT = """
            You read nutrition labels for a vegetarian user's nutrition tracker. The photo shows
            the nutrition facts table, the packaging or the ingredient list of one product.
            Return exactly one part for that product. Read the values, never estimate them.

            Write the part name and all veggie hints in German.

            Part
            - name: the product name, including the brand if it is readable, for example
              "Proteinriegel Schoko (Barebells)".
            - kcalPer100g, proteinPer100g, carbsPer100g, fatPer100g: copy the values per 100 g
              or 100 ml exactly as printed. Use kcal, not kJ. Use total carbohydrates, not the
              "davon Zucker" line. If the label only shows values per serving, convert them to
              100 g using the printed serving size.
            - amountG: the whole package if it is a single serving, for example one bar or one
              bottle, using the printed net weight or volume (treat 1 ml as 1 g). Otherwise use
              the printed serving size. Only if neither is visible, use 100.
            - If a value is not readable, give the typical value for this kind of product.

            Veggie hints
            - If the ingredient list is visible, check it for ingredients that are not or might
              not be vegetarian, for example gelatin, carmine (E120), animal rennet, meat
              extract, fish, or shellac (E904). Name the ingredient in a short German hint.
            - Never claim that the product is vegetarian. If nothing is suspicious or no
              ingredient list is visible, return an empty list.
            - Do not warn about eggs, dairy or honey.
            """;

    private final AnthropicClient client = AnthropicOkHttpClient.fromEnv();

    @Override
    public SuggestMealResponse suggest(String text) {
        return ask(SYSTEM_PROMPT, List.of(ContentBlockParam.ofText(text)), SuggestMealResponse.class);
    }

    @Override
    public SuggestMealResponse suggestFromPhoto(byte[] photo, String text) {
        List<ContentBlockParam> content = new ArrayList<>();
        content.add(imageBlock(photo));
        if (text != null && !text.isBlank()) {
            content.add(ContentBlockParam.ofText(text));
        }
        return ask(SYSTEM_PROMPT, content, SuggestMealResponse.class);
    }

    @Override
    public ReadLabelResponse readLabel(byte[] photo) {
        return ask(LABEL_PROMPT, List.of(imageBlock(photo)), ReadLabelResponse.class);
    }

    private ContentBlockParam imageBlock(byte[] photo) {
        return ContentBlockParam.ofImage(Base64ImageSource.builder()
                .data(Base64.getEncoder().encodeToString(photo))
                .mediaType(Base64ImageSource.MediaType.IMAGE_JPEG)
                .build());
    }

    private <T> T ask(String systemPrompt, List<ContentBlockParam> content, Class<T> responseType) {
        StructuredMessageCreateParams<T> params = MessageCreateParams.builder()
                .model(Model.CLAUDE_SONNET_5_5)
                .maxTokens(2048)
                .system(systemPrompt)
                .addUserMessageOfBlockParams(content)
                .outputConfig(responseType)
                .build();

        StructuredMessage<T> message = client.messages().create(params);

        return message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(StructuredTextBlock::text)
                .findFirst()
                .orElseThrow();
    }
}
