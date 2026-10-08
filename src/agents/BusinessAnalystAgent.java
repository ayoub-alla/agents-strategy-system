package agents;

import app.GeminiAPI;
import app.Config;

/** Analyzes problem, users, objectives, resources and requirements. */

//since the 3 agents BusinessAnalystAgent , MarketRiskAgent , StrategyAgent , have some stracture of recieving messages and send them back
//we extend A WorkerAgent class to avoid duplications
private static final String PROMPT = """
            Vous êtes un senior business analyst. Analysez cette idée de projet :
            
            %s

            Renvoyez un objet JSON avec exactement ces clés :
            "problemDefinition": une string sous forme de paragraphe décrivant le problème et son importance
            "targetUsers": un array de segments utilisateurs, chacun avec leurs besoins et pain points
            "objectives": un array d'objectifs de projet mesurables
            "businessAnalysis": un array de ressources requises, exigences fonctionnelles et contraintes
            """;
    
    // the id for conversation of businessanalyst agent
    @Override protected String topic() { return Config.BUSINESS; }

    @Override
    protected String process(String idea) throws Exception {
        return GeminiAPI.generateJson(PROMPT.formatted(idea) + FORMAT_RULES);
    }
}
