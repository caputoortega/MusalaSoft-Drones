package ar.com.caputo.drones.rest;

import static spark.Spark.get;
import static spark.Spark.patch;

import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;

import ar.com.caputo.drones.DroneService;
import ar.com.caputo.drones.database.model.BaseEntityModel;
import ar.com.caputo.drones.database.repo.BaseCrudRepository;
import ar.com.caputo.drones.exception.RequestProcessingException;
import ar.com.caputo.drones.exception.ResourceNotFoundException;
import ar.com.caputo.drones.exception.UnmetConditionsException;

public abstract class RestfulEndpoint<T extends BaseEntityModel> {
    
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

    public void updateObject() {
        patch(BASE_ENDPOINT + "/:id", (req, resp) -> {

            JsonObject requestBody = DroneService.GSON.fromJson(req.body(), JsonObject.class);

            T toUpdate;
            
            try {
                toUpdate = repository.get(req.params(":id"));
        
                requestBody.entrySet().stream()
                .map((entry) -> entry.getKey())
                .collect(Collectors.toUnmodifiableList()).forEach(attribute -> {
                    toUpdate.update(attribute, requestBody.get(attribute));
                });

            } catch (ResourceNotFoundException ex) {
                resp.status(404);
                return null;
            } catch (UnmetConditionsException ex) {
                resp.status(400);
                return buildResponse(ex.getMessage()); // this was sanitised
            } catch (RequestProcessingException ex) {
                resp.status(500);
                return buildResponse(ex.getMessage()); // this was sanitised
            }

            repository.update(toUpdate);
            return buildResponse(toUpdate);

        });
    }

    public abstract void deleteObject();

    protected final String buildResponse(Object data) {
        return DroneService.GSON.toJson(Map.of("data", data));
    }

    protected final String buildBulkResponse(int bulkSize, Object data) {
        return DroneService.GSON.toJson(Map.of("bulkSize", bulkSize, "data", data));
    }

}
