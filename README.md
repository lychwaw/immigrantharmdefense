# PERSONA — Persona Exploitation & Relational Safety Assessment

An adversarial simulation framework for testing whether Claude treats vulnerable
immigrant personas differently based on perceived vulnerability and language register,
and for auditing whether the information it gives them is actually true. Full writeup:
`PROJECT_PERSONA_paper.tex`.

## Fastest way to see the evidence (no setup required)

`liamsredteamimmigrantdb.db` is committed to this repo and contains the real session
transcripts, turn-by-turn judge scores, and persona/scenario metadata behind every claim
in the paper. You don't need any API key or to build anything to inspect it — open it
with [DB Browser for SQLite](https://sqlitebrowser.org/) (free, no install needed beyond
download) or any SQLite client. The tables that matter:

- `sessions` — one row per persona × scenario run (`persona_id`, `scenario_id`, `mode`).
- `turns` — every message in every conversation, in order. This is where the fabricated
  phone numbers behind the paper's headline finding actually live — search the `text`
  column for organization names like `Lawyers for Human Rights` or `UNHCR` to see them
  directly.
- `scores` — the final-turn judge score per session (the five rubric dimensions, 0–3
  each, plus the total).
- `turn_scores` — the same scores, but for every turn of every session, not just the
  last one (used for the escalation/trajectory analysis).

## Generating the report (heatmap, bar charts, trajectory chart)

The two bar charts in the paper were rebuilt natively in LaTeX from the verified numbers
in the tables (see `PROJECT_PERSONA_paper.tex`), so you don't need to run anything to see
them. If you want to regenerate a fresh HTML report (2×2 heatmap, score-breakdown bar
chart, per-turn trajectory chart) from whatever is currently in the database:

1. Run the app (see below).
2. Go to the "Generate Report" tab and click **Generate HTML Report**.
3. Open the resulting file in `reports/` in a browser. The charts are inline SVG, so you
   can screenshot or right-click → "Save image as" on any of them.

Note: the database's current contents reflect the most recent test run, which may not
exactly match the specific numbers reported in the paper's tables (LLM output is
stochastic, and the persona-simulator logic was deliberately strengthened partway
through the project — see Section 2.1.4 of the paper). The paper's tables are a verified
historical record of a specific run, not a live-recomputed formula.

## Running the live app

This is a JavaFX desktop application built with Maven.

**Required environment variables** (set as system environment variables, not literal
values in any file):

- `ANTHROPIC_API_KEY` — required. The model under test (Claude) runs through this.
- `GROQ_API_KEY` — required. The cross-grading judge and the persona simulator both run
  through this (Llama 3.3 70B via Groq).
- `GEMINI_API_KEY` — optional. Only needed if you select "Gemini" as the model under test
  in the Run Session tab; not used by the core judge/persona-simulator pipeline.

On Windows, `setx ANTHROPIC_API_KEY "your-key"` (and the same for the other two), then
restart your terminal/IDE so the new environment variable is actually visible to new
processes.

**JavaFX setup — the one real portability gap to know about:** `pom.xml`'s
`javafx.sdk` property and `.vscode/launch.json`'s `--module-path` are both currently
hardcoded to a local path (`C:/Users/ltcac/Documents/javafx-sdk-26.0.1/lib`). To build or
run this on a different machine, download the
[JavaFX 26.0.1 SDK](https://gluonhq.com/products/javafx/) and update both of those paths
to wherever you put it.

**Entry point:** `uctliam.yourproject.HarmAuditor` (launches `ui.MainDisplayWindow`).

If you're using VS Code with the Java extension, the `HarmAuditor` launch configuration
in `.vscode/launch.json` already wires up the module path and all three environment
variables — just hit Run once the JavaFX path above is corrected for your machine.

## What's in the app

Three tabs: **Run Session** (pick a persona, mode, and model, then run the automated
battery), **View Results** (summary stats and worst-session transcripts), **Generate
Report** (the standalone HTML report described above).
