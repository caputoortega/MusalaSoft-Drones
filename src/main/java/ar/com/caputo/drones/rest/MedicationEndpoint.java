package ar.com.caputo.drones.rest;

import static spark.Spark.post;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;

import ar.com.caputo.drones.DroneService;
import ar.com.caputo.drones.database.model.Medication;
import ar.com.caputo.drones.database.repo.MedicationRepository;
import ar.com.caputo.drones.exception.UnmetConditionsException;

public class MedicationEndpoint extends RestfulEndpoint<Medication> {

    public MedicationEndpoint() {
        super("/medications", new MedicationRepository(DroneService.getInstance().getDataSource()));
    }

    @Override
    public void addObject() {

        post(BASE_ENDPOINT, "application/json", (req, resp) -> {

            JsonObject requestBody = DroneService.GSON.fromJson(req.body(), JsonObject.class);

            /*
               Checking whether the request body contains all required
               params to create a new Medication
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

            Medication toCreate = new Medication();

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
    public void updateObject() {
    }

    @Override
    public void deleteObject() {
    }
    
}
