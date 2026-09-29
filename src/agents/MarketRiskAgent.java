package agents;

import app.GeminiAPI;
import app.Config;

/** Analyzes competitors, opportunities, threats and risks. */

//since the 3 agents BusinessAnalystAgent , MarketRiskAgent , StrategyAgent , have some stracture of recieving messages and send them back
//we extend A WorkerAgent class to avoid duplications

public class MarketRiskAgent extends WorkerAgent {

    private static final String PROMPT = """
            You are a market and risk analyst , Analyze this project idea:
            
            %s

            Return a JSON object with exactly these keys:
            "marketAnalysis": an array covering market size/trends, competitors, opportunities and threats
            "risks": an array of objects, each with keys "risk", "likelihood" (Low/Medium/High),
                     "impact" (Low/Medium/High) and "mitigation"
            """;

    // the id for conversation of marketrisk agent
    @Override protected String topic() { return Config.MARKET; }

    @Override
    protected String process(String idea) throws Exception {
        return GeminiAPI.generateJson(PROMPT.formatted(idea) + FORMAT_RULES);
    }
}