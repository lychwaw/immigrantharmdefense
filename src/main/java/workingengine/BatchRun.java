package workingengine;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import data.JSONLoader;
import model.Persona;
import model.Scenario;

public class BatchRun {

    public static String runAll(int turnsPerSession, Persona personaFilter, String mode,
                                 Supplier<ConvoEngine> engineFactory, Consumer<String> onProgress) {
        StringBuilder summary = new StringBuilder();  // summary of the batch run results, will be displayed
        try {
            List<Persona> personas = JSONLoader.loadPersonas();
            List<Scenario> scenarios = JSONLoader.loadScenarios(); // loads personas and scenarios

            if (personaFilter != null) {
                personas = personas.stream().filter(p -> p.id.equals(personaFilter.id)).toList();
            }

            int expectedTotal = personas.size() * scenarios.size();
            int total = 0; // starts at 0, increments for each session run
            for (Persona persona : personas) {
                for (Scenario scenario : scenarios) {
                    ConvoEngine engine = engineFactory.get();
                    engine.runAutomatedSession(persona, scenario, turnsPerSession, mode);
                    total++;
                    String line = "Completed " + total + "/" + expectedTotal + ": "
                                + persona.name + " x " + scenario.domain + "\n";
                    summary.append(line); // appends results of automated sessions
                    if (onProgress != null) {
                        onProgress.accept(line); // lets the UI show progress as each session finishes, not just at the very end
                    }
                }
            }
            summary.insert(0, "Batch complete. " + total + " sessions run.\n\n");
        } catch (Exception e) {
            summary.append("Batch error: ").append(e.getMessage());
        }
        return summary.toString();
    }
}

