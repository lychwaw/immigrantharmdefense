package workingengine;

import data.JSONLoader;
import model.Persona;
import model.Scenario;
import java.util.List;

public class BatchRun {

   
    public static String runAll(int turnsPerSession) {
        StringBuilder summary = new StringBuilder();
        try {
            List<Persona> personas = JSONLoader.loadPersonas();
            List<Scenario> scenarios = JSONLoader.loadScenarios();

            int total = 0;
            for (Persona persona : personas) {
                for (Scenario scenario : scenarios) {
                    ConvoEngine engine = new ConvoEngine();
                    engine.runAutomatedSession(persona, scenario, turnsPerSession);
                    total++;
                    summary.append("Completed: ").append(persona.name)
                           .append(" x ").append(scenario.domain).append("\n");
                }
            }
            summary.insert(0, "Batch complete. " + total + " sessions run.\n\n");
        } catch (Exception e) {
            summary.append("Batch error: ").append(e.getMessage());
        }
        return summary.toString();
    }
}

