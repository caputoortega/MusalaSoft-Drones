package ar.com.caputo.drones;

import java.lang.System.Logger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;

import ar.com.caputo.drones.rest.DroneEndpoint;
import ar.com.caputo.drones.rest.MedicationEndpoint;
import ar.com.caputo.drones.task.BatteryLevelLogTask;
import spark.Spark;

public class DroneService {

    private final int FALLBACK_API_PORT = 8080;
    private final String FALLBACK_API_ADDRESS = "0.0.0.0";
    private String DB_NAME = "drones";
    private final String DB_URL = "jdbc:h2:file://" + System.getProperty("user.dir") + "/";

    private final String API_VERSION = "v1";
    public final String API_URL = String.format("/api/%s", API_VERSION);

    public static final Gson GSON = new Gson(); 
    private ConnectionSource source;

    private ScheduledExecutorService scheduler;
    
    /**
     * 4 minutes default logInterval for {@link BatteryLevelLogTask}
     */
    private long logInterval = 240;
    private final Logger BATTERY_AUDIT_LOGGER = System.getLogger("BATTERY AUDIT LOG");
    
    private static DroneService instance;
    private DroneEndpoint droneEnpoint;
    private MedicationEndpoint medicationEndpoint;
    private DroneService() {}

    public static DroneService getInstance() {
        if(instance == null) instance = new DroneService();
        return instance;
    }
    
    public static void main(String... args) {

        String apiAddress = null;
        int apiPort = -1;
        String dbName = null;
        int logInterval = 240;

        if(args.length > 0) {

            for(String arg : args) {
                String[] argData = arg.split(":");
                String argName = argData[0];
                switch (argName) {
                    case "--apiHost":
                    case "-h"       : {
                        apiAddress = argData[1];
                        break;
                    }
                    case "--apiPort":
                    case "-p"       : {
                        apiPort = Integer.parseInt(argData[1]);
                        break;
                    }
                    case "--dbName":
                    case "-db"     : {
                        dbName = argData[1];
                        break;
                    }
                    case "--logInterval":
                    case "-li"          :
                        logInterval = Integer.parseInt(argData[1]);
                        break;
                }
            }
            
        }

        DroneService.getInstance().configure(apiAddress, apiPort, dbName, logInterval);
    
    }

    private void configure(final String API_ADDRESS, final int API_PORT, final String USER_PROVIDED_DB_NAME, final int LOG_INTERVAL) {

        this.logInterval = LOG_INTERVAL;

        String dbName = null; 
        if(USER_PROVIDED_DB_NAME != null) dbName = USER_PROVIDED_DB_NAME.strip();

        this.DB_NAME =
                (dbName == null || dbName.isEmpty() || dbName.equals(this.DB_NAME))
                ? this.DB_NAME
                : dbName;

        /*
         * Spark configuration
         */
        Spark.ipAddress(API_ADDRESS == null ? FALLBACK_API_ADDRESS : API_ADDRESS);
        Spark.port(API_PORT == -1 ? FALLBACK_API_PORT : API_PORT);

        /*
         * Forces every response to be encoded in JSON
         */
        Spark.defaultResponseTransformer((response) -> {
            if(response == null) return "{}";
            return GSON.toJson(JsonParser.parseString(response.toString()));
        });

        Spark.before((req, resp) -> { 
            resp.type("application/json");
        });

        /*
         * Endpoint registration
         */

        this.droneEnpoint = new DroneEndpoint();
        this.medicationEndpoint = new MedicationEndpoint();
        

        /*
         * Init tasks
         */
        
        BatteryLevelLogTask batteryTask = new BatteryLevelLogTask();
        batteryTask.init();

        // Ensures Spark shuts down properly
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            
            
            batteryTask.shutdown();
            getScheduler().shutdown();
            
            Spark.stop();

        }));
    
    }

    public ConnectionSource getDataSource() {
        if(this.source == null) {

            try(ConnectionSource source = new JdbcConnectionSource(DB_URL + DB_NAME)) {

                ((JdbcConnectionSource) source).setUsername("sa");
                ((JdbcConnectionSource) source).setPassword("");
    
                this.source = source;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return this.source;
    }


    /**
     * Sanitises a String to be JSON-format-parsable
     * @param string
     * @return sanitised string
     */
    public static String sanitise(String string) {

        return string.strip()
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\'", "\\\'");

    } 

    public ScheduledExecutorService getScheduler() {
        if(this.scheduler == null)
            this.scheduler = Executors.newScheduledThreadPool(1);
        return this.scheduler;
    }

    public DroneEndpoint getDroneEnpoint() {
        return droneEnpoint;
    }

    public MedicationEndpoint getMedicationEndpoint() {
        return medicationEndpoint;
    }

    public long getLogInterval() {
        return logInterval;
    }

    public Logger getBatteryAuditLogger() {
        return this.BATTERY_AUDIT_LOGGER;
    }
    
    

}
