package agents;

import app.GeminiAPI;
import app.Config;

/** Analyzes competitors, opportunities, threats and risks. */

//since the 3 agents BusinessAnalystAgent , MarketRiskAgent , StrategyAgent , have some stracture of recieving messages and send them back
//we extend A WorkerAgent class to avoid duplications

public class MarketRiskAgent extends WorkerAgent {

    private static final String PROMPT = """

    Vous êtes un analyste de marché et des risques.
    Analysez l'idée de projet suivante :

    %s

    Retournez un objet JSON contenant exactement les clés suivantes :

    "marketAnalysis" : un tableau présentant la taille du marché, les tendances du marché, les concurrents, les opportunités et les menaces.

    "risks" : un tableau d'objets, chacun contenant les clés suivantes :
        "risk" : description du risque
        "likelihood" : probabilité (Low/Medium/High)
        "impact" : impact (Low/Medium/High)
        "mitigation" : mesures d'atténuation du risque
        """;

    // the id for conversation of marketrisk agent
    @Override protected String topic() { return Config.MARKET; }

    @Override
    protected String process(String idea) throws Exception {
        return GeminiAPI.generateJson(PROMPT.formatted(idea) + FORMAT_RULES);
    }
}