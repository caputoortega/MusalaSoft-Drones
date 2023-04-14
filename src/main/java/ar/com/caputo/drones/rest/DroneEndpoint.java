package ar.com.caputo.drones.rest;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.delete;
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
        bulkAdd();
        getAvailableDrones();
        getBatteryLevel();
        contents();
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
                
                resp.status(201);
                repository.addNew(toCreate); 
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

                JsonObject toBuild = DroneService.GSON.fromJson(jsonDrone, JsonObject.class);                
                if(payloadCanFulfilModel(toBuild)) {

                    try {
                        dronesToCreate.add(new Drone(
                            toBuild.get("serialNumber").getAsString(),
                            toBuild.get("model").getAsString(),
                            toBuild.get("state").getAsString(),
                            toBuild.get("weightLimit").getAsInt(),
                            toBuild.get("batteryLevel").getAsInt()
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
            return buildBulkResponse((int) bulkAddResult.get(0), (List<?>) bulkAddResult.get(1));
        } catch (SQLException ex) {
            resp.status(422);
            return buildResponse(ex.getCause().getMessage());
        }

        });

    }

    @Override
    public void deleteObject() {

        delete(BASE_ENDPOINT + "/:id", PAYLOAD_ENCODING, (req, resp) -> {

            Drone toDelete;
            try {
                toDelete = repository.get(req.params(":id"));
            } catch(ResourceNotFoundException ex) {
                resp.status(404);
                return null;
            }

            /*
              If drone is not IDLE it means it's performing some
              operation, or that the current state is unknown.
              For said reason, and to prevent losing contact with
              it causing a potential incident, deletions can only
              be performed while their state is set to IDLE
            */
            if(toDelete.getState() != Drone.State.IDLE) {
                resp.status(409);
                return buildResponse("Cannot delete drone while not IDLE");
            }
    
            return buildResponse(repository.delete(toDelete.getSerialNumber()));

        });

    }

    public void contents() {

        get(BASE_ENDPOINT + "/:id/items", (req, resp) -> {
            return buildResponse(repository.get(req.params(":id")).getLoad());
        });

    }

}
