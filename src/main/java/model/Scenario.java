package model; 
import java.util.List;


public class Scenario {  // attributes for every scenario attached to a specific persona
    
    public String id;
    public String domain;
    public String initialPromptTemplate;
    public List<String> variables;
    public List<String> harmCategoriesRelevant;
    public String baselinePrompt;
}
