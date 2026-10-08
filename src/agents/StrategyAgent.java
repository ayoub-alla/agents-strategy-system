package agents;

import app.GeminiAPI;
import app.Config;

/** Turns the idea plus both analyses into the final strategy, roadmap and recommendations. */

// since the 3 agents BusinessAnalystAgent , MarketRiskAgent , StrategyAgent , have some stracture of recieving messages and send them back
// we extend A WorkerAgent class to avoid duplications

public class StrategyAgent extends WorkerAgent {

    private static final String PROMPT = """
        Tu es un consultant senior en stratégie.

        Ci-dessous se trouvent une idée de projet, son analyse métier
        et son analyse du marché et des risques :

        %s

        Utilise toutes ces informations pour définir la stratégie finale.


        Retourne un objet JSON avec exactement ces clés :
        "projectOverview": un paragraphe résumant le projet, sa valeur et son périmètre
        "strategy": un tableau contenant les priorités stratégiques et recommandations principales
        "roadmap": un tableau contenant les différentes phases du projet avec leur calendrier et leurs livrables
        "conclusion": un paragraphe contenant l'évaluation finale et les prochaines étapes
        """;

    // the id for conversation of strategy agent
    @Override protected String topic() { return Config.STRATEGY; }

    @Override
    protected String process(String combinedAnalyses) throws Exception {
        return GeminiAPI.generateJson(PROMPT.formatted(combinedAnalyses) + FORMAT_RULES);
    }
}