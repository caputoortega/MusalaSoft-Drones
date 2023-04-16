package ar.com.caputo.drones.rest;

import static spark.Spark.get;
import static spark.Spark.post;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ar.com.caputo.drones.DroneService;
import ar.com.caputo.drones.database.model.Drone;
import ar.com.caputo.drones.database.repo.DroneRepository;
import ar.com.caputo.drones.exception.InvalidBulkItemException;
import ar.com.caputo.drones.exception.InvalidInputFormatException;
import ar.com.caputo.drones.exception.ResourceNotFoundException;
import ar.com.caputo.drones.exception.UnmetConditionsException;

public class DroneEndpoint extends RestfulEndpoint<Drone> {

    public DroneEndpoint() {
        super("/drones", new DroneRepository(DroneService.getInstance().getDataSource()));
    }

    protected void registerRoutes() {
        super.registerRoutes();
        getAvailableDrones();
        getBatteryLevel();
        getItems();
    }

    @Override
    protected final boolean payloadCanFulfilModel(JsonObject payload) {

        Set<String> requiredFields = Set.of(
            "serialNumber",
            "model",
            "state",
            "weightLimit",
            "batteryLevel"
        );
        return payload.keySet().containsAll(requiredFields);

    } 

    /**
     * This endpoint will return all drones available
     * for loading, that is, drones with state IDLE
     * and with battery levels greater or equal than 25.
     */
    public void getAvailableDrones() {

        get(BASE_ENDPOINT + "/available", (req, resp) -> {

            return buildResponse(repository.getDao().queryForEq("state", Drone.State.IDLE).stream()
                    .filter(drone -> drone.getBatteryLevel() >= 25).collect(Collectors.toList()));

        });

    }

    public void getBatteryLevel() {

        get(BASE_ENDPOINT + "/:id/battery", (req, resp) -> {
            
            Drone requestedDrone;

            try {
                requestedDrone = repository.get(req.params(":id"));
            } catch(ResourceNotFoundException ex) {
                resp.status(404);
                return null;
            }

            return buildResponse(requestedDrone.getBatteryLevel());

        });

    }

    @Override
    public void addObject() {

        post(BASE_ENDPOINT, PAYLOAD_ENCODING, (req, resp) -> {

            JsonObject requestBody = DroneService.GSON.fromJson(req.body(), JsonObject.class);
            
           
            if(!payloadCanFulfilModel(requestBody)) {

                resp.status(400);
                return null;

            }

            Drone toCreate;
            try { 
                toCreate = new Drone(
                    requestBody.get("serialNumber").getAsString(),
                    requestBody.get("model").getAsString(),
                    requestBody.get("state").getAsString(),
                    requestBody.get("weightLimit").getAsInt(),
                    requestBody.get("batteryLevel").getAsInt()                
                );           
            } catch(IllegalArgumentException | InvalidInputFormatException ex) {
                resp.status(400);
                return buildResponse(ex.getMessage());
            }

            try { 
                
                repository.addNew(toCreate); 
                resp.status(201);
                return buildResponse(toCreate);

            } catch(UnmetConditionsException ex) {
                resp.status(400);
                
                return buildResponse(ex.getMessage()); //this was sanitised before
            }

        });

    }

    @Override
    public void bulkAdd() {

        post(BASE_ENDPOINT + "/bulk", PAYLOAD_ENCODING, (req, resp) -> {

            JsonObject requestBody = DroneService.GSON.fromJson(req.body(), JsonObject.class); 
            JsonArray bulkData = requestBody.get("bulk").getAsJsonArray();
            List<Drone> dronesToCreate = new ArrayList<>();

            try { 
            bulkData.forEach((jsonDrone) -> {

                JsonObject toCreate = DroneService.GSON.fromJson(jsonDrone, JsonObject.class);                
                if(payloadCanFulfilModel(toCreate)) {

                    try {
                        dronesToCreate.add(new Drone(
                            toCreate.get("serialNumber").getAsString(),
                            toCreate.get("model").getAsString(),
                            toCreate.get("state").getAsString(),
                            toCreate.get("weightLimit").getAsInt(),
                            toCreate.get("batteryLevel").getAsInt()
                        ));
                    } catch (InvalidInputFormatException | IllegalArgumentException e) {
                        throw new RuntimeException(e);
                    }

                } else {
                    throw new RuntimeException(new InvalidBulkItemException(bulkData.toString()));
                }

            });

        } catch(RuntimeException e) {
            if(e.getCause() instanceof InvalidInputFormatException ||
               e.getCause() instanceof InvalidBulkItemException) {
                resp.status(400);
                return buildResponse(e.getCause().getMessage());
            }
            resp.status(500);
            e.printStackTrace();
            return buildResponse(e.getMessage());
        }

        try {
            List<?> bulkAddResult = repository.addNewBulk(dronesToCreate);
            resp.status(201);
            return buildBulkResponse((int) bulkAddResult.get(0), (List<?>) bulkAddResult.get(1));
        } catch (SQLException ex) {
            resp.status(422);
            return buildResponse(ex.getCause().getMessage());
        }

        });

    }

    public void getItems() {

        get(BASE_ENDPOINT + "/:id/items", (req, resp) -> {
            return buildResponse(repository.get(req.params(":id")).getLoad());
        });

    }

    public List<Drone> getAllDrones() {
        return repository.listAll();
    }

}
