# veggietracker-v2

A mobile macro tracker for vegetarians. Describe your meal or take a photo, get a suggestion for the nutrients, check it and save it.

## How it works

1. Describe what you ate ("2 eggs and a slice of wholegrain bread") or take a photo of your plate
2. Claude splits the meal into its parts and suggests kcal, protein, carbs and fat per 100 g plus the amount in grams
3. You check the suggestion, correct it if needed and save it
4. The start page shows today's totals and meals

The AI only suggests. Portion sizes are hard to judge from a photo, so the final value is always yours.

## Veggie hints

The app never says "this is vegetarian". It only shows warnings about things that might not be, for example chicken stock in a risotto or animal rennet in Parmesan. If nothing is found, there is no warning, not a "vegetarian" label. A photo can't show hidden ingredients, so when in doubt, ask or check the label.

## Design decisions

- **Values per 100 g plus amount.** Each part stores its nutrients per 100 g and the eaten amount. Totals are calculated, never stored, so changing an amount can't leave a stale sum behind.
- **Structured output.** Claude answers in the exact shape of a Java record, so there is no manual JSON parsing.
- **The AI sits behind an interface.** `MealSuggester` hides which model or provider is used, so switching is a one-line change.
- **"Today" means today in Vienna.** Day boundaries are calculated in `Europe/Vienna` and stored as UTC timestamps.

## Status

- [x] Log a meal with its parts
- [x] Daily overview with totals and veggie hints
- [x] Describe a meal in text and get a suggestion
- [ ] Take a photo and get a suggestion (backend done, frontend next)

## Roadmap

- [ ] Delete and edit meals
- [ ] Daily goals with progress
- [ ] History and weekly overview
- [ ] WHOOP integration to factor in calories burned
- [ ] Deployment

## Stack

- **Backend:** Java 21, Spring Boot 4, Spring Data JPA, PostgreSQL, Flyway, Anthropic Java SDK
- **Frontend:** Vue 3, TypeScript, Vite, Tailwind CSS, shadcn-vue, built mobile-first for iPhone
- **Testing and CI:** JUnit with Testcontainers, Vitest, GitHub Actions

## Running locally

Requirements: Java 21+, Node 24, Docker and an Anthropic API key.

```bash
docker compose up -d
```

```bash
cd backend && ANTHROPIC_API_KEY=your-key ./mvnw spring-boot:run
```

```bash
cd frontend && npm install && npm run dev
```

The frontend runs on http://localhost:5173 and forwards `/api` to the backend on port 8080.
