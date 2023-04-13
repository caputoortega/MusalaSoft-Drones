package ar.com.caputo.drones.rest;

import static spark.Spark.get;

import java.util.Map;

import ar.com.caputo.drones.DroneService;
import ar.com.caputo.drones.database.repo.BaseCrudRepository;
import ar.com.caputo.drones.exception.ResourceNotFoundException;

public abstract class RestfulEndpoint<T> {
    
    public final String BASE_ENDPOINT;
    protected final BaseCrudRepository<T,String> repository;

    public RestfulEndpoint(final String BASE_ENDPOINT, final BaseCrudRepository<T, String> repo) {
        this.BASE_ENDPOINT = DroneService.getInstance().API_URL + BASE_ENDPOINT;
        this.repository = repo;
        register();
    
    }

    protected void register() {
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

    public abstract void updateObject();

    public abstract void deleteObject();

    protected final String buildResponse(Object data) {
        return DroneService.GSON.toJson(Map.of("data", data));
    }

    protected final String buildBulkResponse(int bulkSize, Object data) {
        return DroneService.GSON.toJson(Map.of("bulkSize", bulkSize, "data", data));
    }

}
