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
import ar.com.caputo.drones.database.model.Medication;
import ar.com.caputo.drones.database.repo.DroneRepository;
import ar.com.caputo.drones.exception.InvalidBulkItemException;
import ar.com.caputo.drones.exception.InvalidInputFormatException;
import ar.com.caputo.drones.exception.ResourceNotFoundException;
import ar.com.caputo.drones.exception.UnmetConditionsException;

public class DroneEndpoint extends RestfulEndpoint<Drone> {

    public DroneEndpoint() {
        super("/drones", new DroneRepository());
    }

    protected void registerRoutes() {
        super.registerRoutes();
        getAvailableDrones();
        getBatteryLevel();
        getItems();

        loadItem();
        unloadItem();

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

    public void loadItem() {

        post(BASE_ENDPOINT + "/:id/items", (req, resp) -> {
                
            JsonObject requestBody = DroneService.GSON.fromJson(req.body(), JsonObject.class);

            String medicationCode = requestBody.get("code").getAsString();
            
            Drone targetDrone = repository.get(req.params(":id"));

            if(targetDrone == null) {
                resp.status(404);
                return null;
            }

            Medication medication = DroneService.getInstance().getMedicationEndpoint().repository.get(medicationCode);

            if(medication == null) {
                resp.status(404);
                return buildResponse("No medication with code ".concat(medicationCode).concat(" could be found!"));
            }

            if(medication.getAssociatedDrone() != null) {
                if(medication.getAssociatedDrone().equals(targetDrone)) {
                    resp.status(200);
                    return buildResponse("Item ".concat(medicationCode).concat(" was already associated to this drone!"));
                }

                resp.status(400);
                return buildResponse("Item ".concat(medicationCode).concat(" is already associated to another drone!"));
            }

            if(targetDrone.canHold(medication.getWeight())) {
                medication.setAssociatedDrone(null);
                resp.status(200);
                return buildResponse("Item ".concat(medicationCode).concat(" was loaded to drone ".concat(targetDrone.id())));
            } else {

                resp.status(422);
                return buildResponse("Item ".concat(medicationCode).concat( " exceeds weight limit for drone ".concat(targetDrone.id())));

            }

        });

    }

    public void unloadItem() {

        delete(BASE_ENDPOINT + "/:id/items", (req, resp) -> {
            
            JsonObject requestBody = DroneService.GSON.fromJson(req.body(), JsonObject.class);

            String medicationCode = requestBody.get("code").getAsString();
            
            Drone targetDrone = repository.get(req.params(":id"));

            if(targetDrone == null) {
                resp.status(404);
                return null;
            }

            Medication medication = DroneService.getInstance().getMedicationEndpoint().repository.get(medicationCode);

            if(medication == null) {
                resp.status(404);
                return buildResponse("No medication with code ".concat(medicationCode).concat(" could be found!"));
            }

            if(medication.getAssociatedDrone() == null) {
                resp.status(400);
                return buildResponse("Item ".concat(medicationCode).concat(" doesn't have any drones associated!"));
            }

            if(medication.getAssociatedDrone().equals(targetDrone)) {
                medication.setAssociatedDrone(null);
                resp.status(200);
                return buildResponse("Item ".concat(medicationCode).concat(" was unloaded from drone ".concat(targetDrone.id())));
            } else {

                resp.status(400);
                return buildResponse("Item ".concat(medicationCode).concat( " is not associated to drone ".concat(targetDrone.id())));

            }

        });

    }

}
