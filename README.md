# Refuel

An AI calorie tracker for vegetarians, built for the iPhone. Describe your meal, take a photo of your plate or scan a nutrition label, check the suggestion and save it. Works with WHOOP: your daily calorie goal follows what you actually burned.

## How it works

1. Describe what you ate ("2 eggs and a slice of wholegrain bread"), take a photo of your plate, or scan the nutrition label of a single product
2. Claude splits the meal into its parts and suggests kcal, protein, carbs and fat per 100 g plus the amount in grams
3. You check the suggestion, correct it if needed and save it
4. The start page shows calories against your goal, protein against your protein goal and what you burned today

The AI only suggests. Portion sizes are hard to judge from a photo, so the final value is always yours.

## Works with WHOOP

Connect your WHOOP account once in the settings. Refuel then reads your energy expenditure and uses the average of the last 7 days as your daily calorie goal, plus what you burned so far today. Without WHOOP, you enter your average burn by hand.

- **OAuth 2.0** with a `state` check against CSRF, tokens stored in the database
- **Automatic token refresh.** WHOOP rotates refresh tokens, so every refresh stores the new one, and refreshes are synchronized so two requests can't use the same token
- **Days, not cycles.** WHOOP measures from sleep to sleep. A night without detected sleep makes one cycle last 34 hours, so the weekly average counts the cycles that started in the last 7 days instead of the last 7 cycles. The result matches the WHOOP app.

Refuel is an independent project and is not affiliated with or endorsed by WHOOP.

## Goals

- **Mode:** Cut, Maintain or Bulk. Calories turn green when you are below, around (±5 %) or above your goal, and red otherwise
- **Protein:** 2 g per kg of body weight

## Veggie hints

The app never says "this is vegetarian". It only shows warnings about things that might not be, for example chicken stock in a risotto, animal rennet in Parmesan or gelatin on an ingredient list. If nothing is found, there is no warning, not a "vegetarian" label. A photo can't show hidden ingredients, so when in doubt, ask or check the label.

## Design decisions

- **Values per 100 g plus amount.** Each part stores its nutrients per 100 g and the eaten amount. Totals are calculated, never stored, so changing an amount can't leave a stale sum behind.
- **Structured output.** Claude answers in the exact shape of a Java record, so there is no manual JSON parsing.
- **The AI sits behind an interface.** `MealSuggester` hides which model or provider is used, so switching is a one-line change.
- **Photos are resized on the phone.** The frontend converts every photo to a JPEG of at most 1600 px before uploading. This keeps uploads small and handles iPhone HEIC photos.
- **"Today" means today in Vienna.** Day boundaries are calculated in `Europe/Vienna` and stored as UTC timestamps.

## Status

- [x] Log a meal with its parts, delete meals
- [x] AI suggestions from text, photo and nutrition label
- [x] Veggie hints
- [x] Daily goals for calories and protein
- [x] WHOOP integration
- [ ] Weight history with a chart
- [ ] Edit meals, history and weekly overview
- [ ] Login and deployment

## Stack

- **Backend:** Java 21, Spring Boot 4, Spring Data JPA, PostgreSQL, Flyway, Anthropic Java SDK, Spring RestClient
- **Frontend:** Vue 3, TypeScript, Vite, Tailwind CSS, built mobile-first for iPhone
- **Testing and CI:** JUnit with Testcontainers, Vitest, GitHub Actions

## Running locally

Requirements: Java 21+, Node 24, Docker and an Anthropic API key. WHOOP is optional and needs an app from the [WHOOP developer dashboard](https://developer-dashboard.whoop.com) with the redirect URL `http://localhost:5173/api/whoop/callback` and the scope `read:cycles`.

```bash
docker compose up -d
```

```bash
cd backend && ANTHROPIC_API_KEY=your-key WHOOP_CLIENT_ID=your-id WHOOP_CLIENT_SECRET=your-secret ./mvnw spring-boot:run
```

```bash
cd frontend && npm install && npm run dev
```

The frontend runs on http://localhost:5173 and forwards `/api` to the backend on port 8080.
