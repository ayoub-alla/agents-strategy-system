package agents;

import app.GeminiAPI;
import app.Config;

/** Turns the idea plus both analyses into the final strategy, roadmap and recommendations. */

// since the 3 agents BusinessAnalystAgent , MarketRiskAgent , StrategyAgent , have some stracture of recieving messages and send them back
// we extend A WorkerAgent class to avoid duplications

public class StrategyAgent extends WorkerAgent {

    private static final String PROMPT = """
    		
            You are a senior strategy consultant. Below are a project idea, its business analysis
            and its market/risk analysis (JSON):
            
            %s

            Use ALL of it to define the final strategy. Return a JSON object with exactly these keys:
            "projectOverview": a paragraph string summarizing the project, its value and scope
            "strategy": an array of proposed strategy priorities and key recommendations
            "roadmap": an array of phased roadmap steps, each naming its timeline and deliverables
            "conclusion": a paragraph string with a closing assessment and immediate next steps
            
            """;

    // the id for conversation of strategy agent
    @Override protected String topic() { return Config.STRATEGY; }

    @Override
    protected String process(String combinedAnalyses) throws Exception {
        return GeminiAPI.generateJson(PROMPT.formatted(combinedAnalyses) + FORMAT_RULES);
    }
}