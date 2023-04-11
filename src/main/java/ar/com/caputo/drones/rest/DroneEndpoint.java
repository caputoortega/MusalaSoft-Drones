package ar.com.caputo.drones.rest;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.delete;
import static spark.Spark.patch;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;

import ar.com.caputo.drones.DroneService;
import ar.com.caputo.drones.database.model.Drone;
import ar.com.caputo.drones.database.repo.DroneRepository;
import ar.com.caputo.drones.exception.RequestProcessingException;
import ar.com.caputo.drones.exception.ResourceNotFoundException;
import ar.com.caputo.drones.exception.UnmetConditionsException;

public class DroneEndpoint extends RestfulEndpoint<Drone> {

    public DroneEndpoint() {
        super("/drones", new DroneRepository(DroneService.getInstance().getDataSource()));
    }

    protected void register() {
        super.register();
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

        get(BASE_ENDPOINT + "/available/", (req, resp) -> {

            return repository.getDao().queryForEq("state", Drone.State.IDLE).stream()
                    .filter(drone -> drone.getBatteryLevel() >= 25).collect(Collectors.toList());

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

            return "{\"batteryLevel\":" + requestedDrone.getBatteryLevel() + "}" ;

        });

    }

    @Override
    public void addObject() {

        post(BASE_ENDPOINT, "application/json", (req, resp) -> {

            JsonObject requestBody = DroneService.GSON.fromJson(req.body(), JsonObject.class);

            /*
               Checking whether the request body contains all required
               params to create a new Drone
            */
            boolean isValidRequest = requestBody.entrySet().stream()
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

            if(!isValidRequest) {

                resp.status(400);
                return null;

            }

            Drone toCreate = new Drone(
                requestBody.get("serialNumber").getAsString(),
                requestBody.get("model").getAsString(),
                requestBody.get("state").getAsString(),
                requestBody.get("weightLimit").getAsInt(),
                requestBody.get("batteryLevel").getAsInt()                
            );           

            try { 
                
                repository.addNew(toCreate); 
                return toCreate;

            } catch(UnmetConditionsException ex) {
                resp.status(400);
                
                return "{ \"result\": \"" + DroneService.sanitise(ex.getMessage()) + "\" }";
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
                return "{\"result\": \"Cannot delete drone while not IDLE\"}";
            }
    
            return "{\"result\":" + this.repository.delete(toDelete.getSerialNumber()) + "}";

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

                try {

                    Class<?> attributeFieldClass = Drone.class.getDeclaredField(attribute).getType();

                    // Getting the PUBLIC setter method for the given attribute
                    Method toUpdateMethod = Drone.class.getMethod("set" + capitalisedAttribute,
                            attributeFieldClass.isEnum() ? String.class : attributeFieldClass);
                    // Setters only have a single param
                    Type paramType = toUpdateMethod.getGenericParameterTypes()[0];

                    /*
                     * For this specific implementation it is safe
                     * to asume that if the argument is not an integer
                     * then we can safely treat it as a String
                     */
                    if(paramType.getTypeName().equals("int"))
                         toUpdateMethod.invoke(toUpdate, requestBody.get(attribute).getAsInt());
                    else toUpdateMethod.invoke(toUpdate, requestBody.get(attribute).getAsString());
                    
                } catch (InvocationTargetException ex) {
                    throw new UnmetConditionsException(ex.getTargetException().getMessage());
                } catch (Exception ex) {
                    throw new RequestProcessingException(ex.toString());
                } 
            
            });

            } catch (UnmetConditionsException ex) {
                resp.status(400);
                return "{ \"result\": \"" + DroneService.sanitise(ex.getMessage()) + "\" }";
            } catch (RequestProcessingException ex) {
                resp.status(500);
                return "{ \"result\": \"" + DroneService.sanitise(ex.getMessage()) + "\" }";
            }

            repository.update(toUpdate);
            return toUpdate;

        });

    }

    public void contents() {

        get(BASE_ENDPOINT + "/:id/items", (req, resp) -> {
            return null;
        });

    }

}
