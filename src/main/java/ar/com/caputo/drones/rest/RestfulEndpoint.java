package ar.com.caputo.drones.rest;

import static spark.Spark.get;
import static spark.Spark.patch;
import static spark.Spark.delete;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;

import ar.com.caputo.drones.DroneService;
import ar.com.caputo.drones.database.model.BaseEntityModel;
import ar.com.caputo.drones.database.repo.BaseCrudRepository;
import ar.com.caputo.drones.exception.InvalidInputFormatException;
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
        bulkAdd();
        addObject();
        updateObject();
        deleteObject();
    }

    /**
     * Retrieves a list with all the objects on the
     * database plus their details
     */
    public void baseGet() {
        get(BASE_ENDPOINT, (req, resp) -> {
            
            return buildResponse(repository.listAll());
        
        });
    }


    /**
     * Retrieves the details for a specific object
     * on the database 
     */
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

    /**
     * Adds an object to the database
     */
    public abstract void addObject();

     /**
     * Add several objects to the database.
     * This endpoint is an <b>all-or-nothing</b> endpoint,
     * if any error is present on the payload, the entire
     * bulk is rejected. 
     */
    public abstract void bulkAdd();

    /**
     * Update a specific object's fields from the database
     */
    public void updateObject() {
        
        patch(BASE_ENDPOINT + "/:id", (req, resp) -> {

            JsonObject requestBody = DroneService.GSON.fromJson(req.body(), JsonObject.class);

            String id = req.params(":id");
            T toUpdate;
            try {
                
                toUpdate = repository.get(id);
        
                requestBody.entrySet().stream()
                .map((entry) -> entry.getKey())
                .collect(Collectors.toUnmodifiableList()).forEach(attribute -> {
                    toUpdate.update(attribute, requestBody.get(attribute));
                });


                repository.update(toUpdate);
                return buildResponse(toUpdate);


            } catch (ResourceNotFoundException ex) {
                resp.status(404);
                return null;
            } catch (UnmetConditionsException | InvalidInputFormatException | SQLException ex) {
                resp.status(400);
                return buildResponse(ex.getMessage()); // this was sanitised
            } catch (RequestProcessingException ex) {
                resp.status(500);
                return buildResponse(ex.getMessage());
            }

        });
    }

    /**
     * Delete a specific object from the database
    */
    public void deleteObject() {

        delete(BASE_ENDPOINT + "/:id", PAYLOAD_ENCODING, (req, resp) -> {

            T toDelete;
            try {

                toDelete = repository.get(req.params(":id"));

                if(toDelete.canBeDeleted())
                    return buildResponse(repository.delete(toDelete.id()));

            } catch(ResourceNotFoundException ex) {
                resp.status(404);
                return null;
            } catch(RequestProcessingException ex) {
                resp.status(409);
                return buildResponse(ex.getMessage());
            }

            resp.status(500);
            return null;


        });
    }

    /**
     * Build a standardised JSON response.
     * This response contains a single key {@code data}
     * that contains all the object's information
     */
    protected final String buildResponse(Object data) {
        return DroneService.GSON.toJson(Map.of("data", data));
    }

    /**
     * Build a standardised JSON bulk response.
     * This response contains two keys, {@code bulkSize},
     * which is the size of objects from the bulk response,
     * and {@code data}, which is the list of objects from
     * the bulk
     */
    protected final String buildBulkResponse(int bulkSize, List<?> data) {
        return DroneService.GSON.toJson(Map.of("bulkSize", bulkSize, "data", data));
    }

    public BaseCrudRepository<T, String> getRepository() {
        return this.repository;
    }
}
