package agents;

import app.GeminiAPI;
import app.Config;

/** Turns the idea plus both analyses into the final strategy, roadmap and recommendations. */

// since the 3 agents BusinessAnalystAgent , MarketRiskAgent , StrategyAgent , have some stracture of recieving messages and send them back
// we extend A WorkerAgent class to avoid duplications

public class StrategyAgent extends WorkerAgent {

    private static final String PROMPT = """
    		
        Vous êtes un consultant senior en stratégie. Vous trouverez ci-dessous une idée de projet, son analyse commerciale
        et son analyse du marché et des risques (au format JSON) :

        %s

        Utilisez TOUTES ces informations pour définir la stratégie finale.

        Retournez un objet JSON contenant exactement les clés suivantes :

        "projectOverview" : un paragraphe résumant le projet, sa valeur ajoutée et son périmètre.

        "strategy" : un tableau présentant les priorités stratégiques proposées et les principales recommandations.

        "roadmap" : un tableau décrivant les étapes du plan de mise en œuvre, chacune précisant son calendrier et ses livrables.

        "conclusion" : un paragraphe présentant l'évaluation finale du projet et les prochaines actions à entreprendre. """ ;
            


    // the id for conversation of strategy agent
    @Override protected String topic() { return Config.STRATEGY; }

    @Override
    protected String process(String combinedAnalyses) throws Exception {
        return GeminiAPI.generateJson(PROMPT.formatted(combinedAnalyses) + FORMAT_RULES);
    }
}