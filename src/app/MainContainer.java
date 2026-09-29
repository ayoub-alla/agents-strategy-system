package app;


import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;


/** Starts the JADE platform and launches the agents. Nothing else. */
public class MainContainer {

    public static void main(String[] args) throws Exception {
    	
    	
    	// create 1 container (mainContaine)
        Runtime runtime = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.GUI, "false"); 
        AgentContainer container = runtime.createMainContainer(profile);

    	//one line
        //AgentContainer container = Runtime.instance().createMainContainer(new ProfileImpl());

        // container.createNewAgent( name , agent class (where setup action ..) , Args ) and start
        
        container.createNewAgent(Config.BUSINESS_ANALYST, "agents.BusinessAnalystAgent" , null).start();
        container.createNewAgent(Config.MARKET_RISK, "agents.MarketRiskAgent" , null).start();
        container.createNewAgent(Config.STRATEGY_AGENT, "agents.StrategyAgent" , null).start();
        container.createNewAgent(Config.REPORT_AGENT, "agents.ReportAgent" , null).start();
        
       // manager start of the flow
        container.createNewAgent(Config.MANAGER, "agents.ManagerAgent" , null).start();
        

       
    }

  
}
