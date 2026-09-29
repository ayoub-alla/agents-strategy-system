package agents;

import app.GeminiAPI;
import app.Config;

/** Analyzes problem, users, objectives, resources and requirements. */

//since the 3 agents BusinessAnalystAgent , MarketRiskAgent , StrategyAgent , have some stracture of recieving messages and send them back
//we extend A WorkerAgent class to avoid duplications

public class BusinessAnalystAgent extends WorkerAgent {

	
    private static final String PROMPT = """
            You are a senior business analyst. Analyze this project idea:
            
            %s

            Return a JSON object with exactly these keys:
            "problemDefinition": a paragraph string describing the problem and why it matters
            "targetUsers": an array of user segments, each with their needs and pain points
            "objectives": an array of measurable project objectives
            "businessAnalysis": an array of required resources, functional requirements and constraints
            """;
    
    // the id for conversation of businessanalyst agent
    @Override protected String topic() { return Config.BUSINESS; }

    @Override
    protected String process(String idea) throws Exception {
        return GeminiAPI.generateJson(PROMPT.formatted(idea) + FORMAT_RULES);
    }
}