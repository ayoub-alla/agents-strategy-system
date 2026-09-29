package app;
import java.io.IOException;
import java.util.logging.*;

/** Single place to tune the whole app: API settings, output location and feature toggles. */
public final class Config {
    private Config() {}
    
    
    // Agent local names
    public static final String MANAGER = "manager";
    public static final String BUSINESS_ANALYST = "businessAnalyst";
    public static final String MARKET_RISK = "marketRisk";
    public static final String STRATEGY_AGENT = "strategyAgent";
    public static final String REPORT_AGENT = "reportAgent";

    // Conversation ids (one per workflow step) used in like 
    // msg.getConversationId() contact between manageraganet and others
    public static final String BUSINESS = "business-analysis";
    public static final String MARKET = "market-risk-analysis";
    public static final String STRATEGY = "strategy";
    public static final String REPORT = "report";

    // Read from the environment so the key never lands in source control (or paste it here for local tests).
    public static final String GEMINI_API_KEY = System.getenv().getOrDefault("GEMINI_API_KEY", "api_key_here");

    // Primary model; the fallbacks are tried in order when a model is overloaded (503) or rate limited (429).
    public static final String GEMINI_MODEL = "gemini-3.1-flash-lite";
    public static final String[] GEMINI_FALLBACK_MODELS = { "gemini-3.5-flash" ,"gemini-3.5-flash-lite", "gemini-3.8-flash" , "gemini-3.6-flash" };

    // Minimum gap between two Gemini calls across ALL agents (60000 / your RPM limit).
    public static final long GEMINI_MIN_INTERVAL_MS = 15000;

    public static final String REPORT_NAME = "strategy";   // -> strategy.pdf / strategy.docx
    public static final boolean PDF_ENABLED = true;
    public static final boolean WORD_ENABLED = true;
    
    public static final String OUTPUT_PATH = "output/reports"; // default output folder
    public static final String LOG_FILE = "output/logs/run.log"; // logs file
    public static final String LOGO_PATH = "assets/logo.png"; // local image file; leave blank or missing = skipped, never crashes
    public static final String REPORT_LINK = ""; // shown as a clickable link under the title; blank = skipped
    
}
