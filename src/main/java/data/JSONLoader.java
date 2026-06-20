package data;
import java.io.IOException;
import java.io.Reader;  // importing google/gson libraries for parsing json files
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import model.Persona;
import model.Scenario;

public class JSONLoader {
    private static final Gson gson = new Gson();

    public static List<Persona> loadPersonas() throws IOException {  
        List<Persona> personas = new ArrayList<>();
        Path personaDir = Paths.get("resources/personas");  // now the personas we have created can be referred to
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(personaDir, "*.json")) {
            for (Path entry : stream) {
                try (Reader reader = Files.newBufferedReader(entry)) {
                    Persona p = gson.fromJson(reader, Persona.class);     // then for every path in the directory, it needs to read the json file and convert it to a persona object and add it to the list of personas.
                    personas.add(p);
                }
            }
        }
        return personas;
    }

    public static List<Scenario> loadScenarios() throws IOException {
        List<Scenario> scenarios = new ArrayList<>();
        Path scenarioDir = Paths.get("resources/scenarios");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(scenarioDir, "*.json")) {
            for (Path entry : stream) {
                try (Reader reader = Files.newBufferedReader(entry)) {
                    Scenario s = gson.fromJson(reader, Scenario.class);
                    scenarios.add(s);     // same logic is applied here , except its now for scenarios instead of personas
                }
            }
        }
        return scenarios;
    }
}
