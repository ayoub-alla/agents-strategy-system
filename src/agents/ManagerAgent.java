package agents;

import app.Config;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import org.json.JSONObject;
import utils.Logger;

import java.util.Scanner;

/** Coordinates the workflow: idea -> output path -> analyses (parallel) -> strategy -> report. No AI logic of its own. */
public class ManagerAgent extends Agent {

    private String idea; // intial idea taken from user
    private String outputPath; // output path of the reports taken from user
    private String business;   // JSON from BusinessAnalystAgent
    private String market;     // JSON from MarketRiskAgent
    private boolean strategyRequested;

    @Override
    protected void setup() {
    	
        Object[] args = getArguments(); // agent args
        idea = (args != null && args.length > 0) ? args[0].toString() : askIdea(); // intial prompt 
        
        
        if (idea.isBlank()) { fail("Empty project idea."); return; }


        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {

                Logger.log("user", "idea: " + idea);

                outputPath = askOutputPath();
                Logger.log("manager", "output path: " + outputPath);

                Logger.log("manager", "Starting analysis...");

                request(
                        Config.BUSINESS_ANALYST,
                        Config.BUSINESS,
                        idea
                );

                request(
                        Config.MARKET_RISK,
                        Config.MARKET,
                        idea
                );
            }
        });

      
        // a cyclic behaviour for listening to messages coming , as the manager agent works like an orchestrator
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg == null) { block(); return; }
                try {
                	// the process function of the message
                    processMessage(msg);
                } catch (Exception e) {
                    fail(e.getMessage());
                }
            }
        });
    }

    /** One step of the workflow per incoming message. */
    
    private void processMessage(ACLMessage msg) {
    	
    	
        if (msg.getPerformative() == ACLMessage.FAILURE) {
            fail(msg.getSender().getLocalName() + " failed: " + msg.getContent());
            return;
        }
        
        // receiving messages and check which agent
        if (msg.getPerformative() != ACLMessage.INFORM || msg.getConversationId() == null) return;
        
        switch (msg.getConversationId()) {
        
            case Config.BUSINESS -> business = msg.getContent();
            case Config.MARKET -> market = msg.getContent();
            
            case Config.STRATEGY -> {
                Logger.log("manager", "strategy received, generating report...");
                request(Config.REPORT_AGENT, Config.REPORT, mergeReport(msg.getContent()));
            }
            
            case Config.REPORT -> { Logger.log("manager", msg.getContent()); shutdown(); }
            
            default -> { }
        }

        // strategy needs both analyses, so it's requested only once both have arrived
        if (business != null && market != null && !strategyRequested) {
        	// after the 2 intial agents responded we combine responses as a json to facilitate the part
        	// of strategy agent in reading combined parts and generating his own 
            strategyRequested = true;
            request(Config.STRATEGY_AGENT, Config.STRATEGY, combinedAnalyses());
        }
    }

    private String combinedAnalyses() {
        return new JSONObject()
                .put("idea", idea)
                .put("businessAnalysis", new JSONObject(business))
                .put("marketAndRiskAnalysis", new JSONObject(market))
                .toString();
    }

    /** Flattens idea + outputPath + all three results into one JSON object for ReportAgent. */
    private String mergeReport(String strategyJson) {
        JSONObject report = new JSONObject().put("idea", idea).put("outputPath", outputPath);
        for (String json : new String[] { business, market, strategyJson }) {
            JSONObject part = new JSONObject(json);
            for (String key : part.keySet()) report.put(key, part.get(key));
        }
        return report.toString();
    }

    private void request(String receiver, String topic, String content) {
        ACLMessage m = new ACLMessage(ACLMessage.REQUEST);
        m.addReceiver(new AID(receiver, AID.ISLOCALNAME));
        m.setConversationId(topic);
        m.setContent(content);
        send(m);
    }

    private static String askIdea() {
        System.out.print("Enter your project idea  :");
        return new Scanner(System.in).nextLine().trim();
    }

    // shows the default path from Config and lets the user override it; empty input keeps the default
    private static String askOutputPath() {
        System.out.println("Default output path : " + Config.OUTPUT_PATH);
        System.out.print("If you need to save to a new path put it here (or press Enter to keep default) : ");
        String input = new Scanner(System.in).nextLine().trim();
        return input.isBlank() ? Config.OUTPUT_PATH : input;
    }

    private void fail(String reason) {
        Logger.log("manager", "ERROR: " + reason);
        shutdown();
    }

    private void shutdown() {
        doDelete();
        
        // kill the container from a separate thread: an agent can't kill its own container from inside its own thread
        new Thread(() -> {
            try { getContainerController().kill(); } catch (Exception ignored) { }
            System.exit(0);
        }).start();
    }
}
