package agents;

import app.GeminiAPI;
import app.Config;

/** Analyzes problem, users, objectives, resources and requirements. */

//since the 3 agents BusinessAnalystAgent , MarketRiskAgent , StrategyAgent , have some stracture of recieving messages and send them back
//we extend A WorkerAgent class to avoid duplications

public class BusinessAnalystAgent extends WorkerAgent {


    private static final String PROMPT = """
        Tu es un analyste métier senior. Analyse cette idée de projet :

        %s

        Réponds uniquement en français.

        Retourne un objet JSON avec exactement ces clés :
        "problemDefinition": un paragraphe décrivant le problème et son importance
        "targetUsers": un tableau contenant les segments d'utilisateurs avec leurs besoins et difficultés
        "objectives": un tableau contenant les objectifs mesurables du projet
        "businessAnalysis": un tableau contenant les ressources nécessaires, les exigences fonctionnelles et les contraintes
        """;
    // the id for conversation of businessanalyst agent
    @Override protected String topic() { return Config.BUSINESS; }

    @Override
    protected String process(String idea) throws Exception {
        return GeminiAPI.generateJson(PROMPT.formatted(idea) + FORMAT_RULES);
    }
}
