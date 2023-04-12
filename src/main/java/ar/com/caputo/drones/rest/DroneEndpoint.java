package ar.com.caputo.drones.rest;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.delete;
import static spark.Spark.patch;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ar.com.caputo.drones.DroneService;
import ar.com.caputo.drones.database.model.Drone;
import ar.com.caputo.drones.database.repo.DroneRepository;
import ar.com.caputo.drones.exception.InvalidInputFormatException;
import ar.com.caputo.drones.exception.RequestProcessingException;
import ar.com.caputo.drones.exception.ResourceNotFoundException;
import ar.com.caputo.drones.exception.UnmetConditionsException;

public class DroneEndpoint extends RestfulEndpoint<Drone> {

    public DroneEndpoint() {
        super("/drones", new DroneRepository(DroneService.getInstance().getDataSource()));
    }

    protected void register() {
        super.register();
        bulkAdd();
        getAvailableDrones();
        getBatteryLevel();
        contents();
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

    private final boolean isValidDronePayload(JsonObject payload) {
        return payload.entrySet().stream()
        .map((entry) -> entry.getKey())
        .collect(Collectors.toUnmodifiableList())
        .containsAll(Arrays.asList(
            new String[] {
                "serialNumber",
                "model",
                "state", 
                "weightLimit",
                "batteryLevel"
             }));

    } 

    @Override
    public void addObject() {

        post(BASE_ENDPOINT, "application/json", (req, resp) -> {

            JsonObject requestBody = DroneService.GSON.fromJson(req.body(), JsonObject.class);

            /*
               Checking whether the request body contains all required
               params to create a new Drone
            */
           
            if(!isValidDronePayload(requestBody)) {

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

    /**
     * This endpoint is an everything-or-none endpoint,
     * if any error is present on the payload, the entire
     * bulk is rejected. 
     */
    public void bulkAdd() {

        post(BASE_ENDPOINT + "/bulk", "application/json", (req, resp) -> {

            JsonObject requestBody = DroneService.GSON.fromJson(req.body(), JsonObject.class); 
            JsonArray bulkData = requestBody.get("bulk").getAsJsonArray();
            List<Drone> dronesToCreate = new ArrayList<>();

            try { 
            bulkData.forEach((jsonDrone) -> {

                JsonObject toBuild = DroneService.GSON.fromJson(jsonDrone, JsonObject.class);                
                if(isValidDronePayload(toBuild)) {

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

                }

            });

        } catch(RuntimeException e) {
            if(e.getCause() instanceof InvalidInputFormatException) {
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

        delete(BASE_ENDPOINT + "/:id", "application/json", (req, resp) -> {

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

    @Override
    public void updateObject() {

        patch(BASE_ENDPOINT + "/:id", (req, resp) -> {

            JsonObject requestBody = DroneService.GSON.fromJson(req.body(), JsonObject.class);

            Drone toUpdate;
            try {
                toUpdate = repository.get(req.params(":id"));
            } catch(ResourceNotFoundException ex) {
                resp.status(404);
                return null;
            }

            try {

                requestBody.entrySet().stream()
                .map((entry) -> entry.getKey())
                .collect(Collectors.toUnmodifiableList()).forEach(attribute -> {

                    // We do not update the item collection from this endpoint
                    if(attribute.equalsIgnoreCase("items")) return;

                    String capitalisedAttribute = attribute.substring(0, 1).toUpperCase()
                            + attribute.substring(1, attribute.length());


                        Class<?> attributeFieldClass;
                        Method toUpdateGetterMethod;
                        try {

                            attributeFieldClass = Drone.class.getDeclaredField(attribute).getType();

                            // Getting the PUBLIC setter method for the given attribute
                            toUpdateGetterMethod = Drone.class.getMethod("set" + capitalisedAttribute,
                                        attributeFieldClass.isEnum() ? String.class : attributeFieldClass);

                        } catch(NoSuchFieldException | NoSuchMethodException noSuchEx) {
                            throw new UnmetConditionsException(attribute + " does not exist");
                        }
    
                        // Setters only have a single param
                        Type paramType = toUpdateGetterMethod.getGenericParameterTypes()[0];

                        try { 
                            /*
                            * For this specific implementation it is safe
                            * to asume that if the argument is not an integer
                            * then we can safely treat it as a String
                            */
                            if(paramType.getTypeName().equals("int"))
                                toUpdateGetterMethod.invoke(toUpdate, requestBody.get(attribute).getAsInt());
                            else toUpdateGetterMethod.invoke(toUpdate, requestBody.get(attribute).getAsString());
                        } catch(IllegalArgumentException ex) {
                            throw new UnmetConditionsException(ex.getCause().getMessage());
                        } catch (IllegalAccessException | InvocationTargetException ex) {
                            throw new RequestProcessingException(ex.getMessage());
                        }
            
            });

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

    public void contents() {

        get(BASE_ENDPOINT + "/:id/items", (req, resp) -> {
            return null;
        });

    }

}
