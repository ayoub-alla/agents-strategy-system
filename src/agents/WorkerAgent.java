package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import utils.Logger;

/** Template for agents that serve one kind of REQUEST: receive input, process it,
 *  reply INFORM with the result (or FAILURE with the error text). */

// a shared base for the 3 agents BusinessAnalystAgent , MarketRiskAgent , StrategyAgent  

public abstract class WorkerAgent extends Agent {

    // arrays instead of "- " delimited text: JSON structure separates bullets reliably, text formatting doesn't
    protected static final String FORMAT_RULES = """

            Rules: reply with one JSON object only, no markdown fences. For a field that is a list
            of points, use a JSON array of short plain-text strings (one point per element). For a
            field that is a narrative, use a single plain-text string (one short paragraph).
            
            """;

    protected abstract String topic();

    protected abstract String process(String input) throws Exception;

    @Override
    protected void setup() {
        MessageTemplate mine = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.MatchConversationId(topic()));

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive(mine);
                if (msg == null) { block(); return; }

                Logger.log(getLocalName(), "request received, calling Gemini...");
                ACLMessage reply = msg.createReply(); // keeps receiver + conversation id
                try {
                    String result = process(msg.getContent());
                    reply.setContent(result);
                    reply.setPerformative(ACLMessage.INFORM);
                    Logger.log(getLocalName(), "response: " + result);
                } catch (Exception e) {
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent(String.valueOf(e.getMessage()));
                    Logger.log(getLocalName(), "FAILURE: " + e.getMessage());
                }
                send(reply);
            }
        });
    }
}