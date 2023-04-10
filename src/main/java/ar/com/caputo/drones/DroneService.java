package ar.com.caputo.drones;

import com.google.gson.Gson;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;

import spark.Spark;

public class DroneService {

    private final int FALLBACK_API_PORT = 8080;
    private final String FALLBACK_API_ADDRESS = "0.0.0.0";
    private String DB_NAME = "drones";
    private final String DB_URL = "jdbc:h2:file://" + System.getProperty("user.dir") + "/" + DB_NAME;

    private final String API_VERSION = "v1";
    public final String API_URL = String.format("/api/%s", API_VERSION);

    public static final Gson GSON = new Gson(); 
    private ConnectionSource source;
    
    private static DroneService instance;
    private DroneService() {}

    public static DroneService getInstance() {
        if(instance == null) instance = new DroneService();
        return instance;
    }

    public static void main(String... args) {

        String apiAddress = null;
        int apiPort = -1;
        String dbName = null;

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
                }
            }
            
        }

        DroneService.getInstance().configure(apiAddress, apiPort, dbName);
    
    }

    private void configure(final String API_ADDRESS, final int API_PORT, final String USER_PROVIDED_DB_NAME) {

        this.DB_NAME =
                (USER_PROVIDED_DB_NAME == null || USER_PROVIDED_DB_NAME.equals(this.DB_NAME))
                ? this.DB_NAME
                : USER_PROVIDED_DB_NAME;

        /*
         * Spark configuration
         */
        Spark.ipAddress(API_ADDRESS == null ? FALLBACK_API_ADDRESS : API_ADDRESS);
        Spark.port(API_PORT == -1 ? FALLBACK_API_PORT : API_PORT);

        /*
         * Forces every response to be encoded in JSON
         */
        Spark.defaultResponseTransformer((model) -> {
            if(model == null) return "";
            return DroneService.GSON.toJson(model);
        });

        /*
         * Endpoint registration
         */

        // new DroneEndpoint();
        // new MedicationEndpoint();
        
    
    }

    public ConnectionSource getDataSource() {
        if(this.source == null) {

            try(ConnectionSource source = new JdbcConnectionSource(DB_URL)) {

                ((JdbcConnectionSource) source).setUsername("sa");
                ((JdbcConnectionSource) source).setPassword("");
    
                this.source = source;
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }
        return this.source;
    }

}
