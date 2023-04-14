package ar.com.caputo.drones.rest;

import static spark.Spark.get;

import java.util.Map;

import com.google.gson.JsonObject;

import ar.com.caputo.drones.DroneService;
import ar.com.caputo.drones.database.repo.BaseCrudRepository;
import ar.com.caputo.drones.exception.ResourceNotFoundException;

public abstract class RestfulEndpoint<T> {
    
    public final String BASE_ENDPOINT;
    protected final BaseCrudRepository<T,String> repository;
    protected static final String PAYLOAD_ENCODING = "application/json";

    public RestfulEndpoint(final String BASE_ENDPOINT, final BaseCrudRepository<T, String> repo) {
        this.BASE_ENDPOINT = DroneService.getInstance().API_URL + BASE_ENDPOINT;
        this.repository = repo;
        registerRoutes();
    
    }

     /**
     * Validates whether the given payload is
     * enough to fulfil the model's
     * **not-null field** requirements.
     * IT DOES NOT CHECK FOR INPUT VALIDITY
     */
    protected abstract boolean payloadCanFulfilModel(JsonObject payload);

    protected void registerRoutes() {
        baseGet();
        getObject();
        addObject();
        updateObject();
        deleteObject();
    }


    public void baseGet() {
        get(BASE_ENDPOINT, (req, resp) -> {
            
            return buildResponse(repository.listAll());
        
        });
    }


    public void getObject() {
        get(BASE_ENDPOINT + "/:id", (req, resp) -> {

            try {
                return buildResponse(repository.get(req.params(":id")));
            } catch (ResourceNotFoundException ex) {
                resp.status(404);
                return null;
            }
        });
    }

    public abstract void addObject();

     /**
     * This endpoint is an all-or-nothing endpoint,
     * if any error is present on the payload, the entire
     * bulk is rejected. 
     */
    public abstract void bulkAdd();

    public abstract void updateObject();

    public abstract void deleteObject();

    protected final String buildResponse(Object data) {
        return DroneService.GSON.toJson(Map.of("data", data));
    }

    protected final String buildBulkResponse(int bulkSize, Object data) {
        return DroneService.GSON.toJson(Map.of("bulkSize", bulkSize, "data", data));
    }

}
