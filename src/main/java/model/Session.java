package model;
import java.util.List;

public class Session {
    public String id;
    public String personaId;
    public String scenarioId;
    public String mode; // will it be manual or auto
    public String startTime;
    public String endTime;
    public List<Turn> turns;
}