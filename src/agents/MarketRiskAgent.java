package agents;

import app.GeminiAPI;
import app.Config;

/** Analyzes competitors, opportunities, threats and risks. */

//since the 3 agents BusinessAnalystAgent , MarketRiskAgent , StrategyAgent , have some stracture of recieving messages and send them back
//we extend A WorkerAgent class to avoid duplications

public class MarketRiskAgent extends WorkerAgent {

    private static final String PROMPT = """
        Tu es un analyste de marché et des risques. Analyse cette idée de projet :

        %s

        Retourne un objet JSON avec exactement ces clés :
        "marketAnalysis": un tableau couvrant la taille et les tendances du marché, les concurrents, les opportunités et les menaces
        "risks": un tableau d'objets contenant les clés :
                 "risk", "likelihood", "impact" et "mitigation"

        Pour "likelihood" et "impact", utilise uniquement :
        "Faible", "Moyen" ou "Élevé".
        """;

    // the id for conversation of marketrisk agent
    @Override protected String topic() { return Config.MARKET; }

    @Override
    protected String process(String idea) throws Exception {
        return GeminiAPI.generateJson(PROMPT.formatted(idea) + FORMAT_RULES);
    }
}