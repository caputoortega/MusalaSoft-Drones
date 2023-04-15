package ar.com.caputo.drones.rest;

import static spark.Spark.post;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import ar.com.caputo.drones.DroneService;
import ar.com.caputo.drones.database.model.Medication;
import ar.com.caputo.drones.database.repo.MedicationRepository;
import ar.com.caputo.drones.exception.InvalidBulkItemException;
import ar.com.caputo.drones.exception.InvalidInputFormatException;
import ar.com.caputo.drones.exception.UnmetConditionsException;

public class MedicationEndpoint extends RestfulEndpoint<Medication> {

    public MedicationEndpoint() {
        super("/medications", new MedicationRepository(DroneService.getInstance().getDataSource()));
    }

    @Override
    public void registerRoutes() {
        super.registerRoutes();
    }


    @Override
    protected final boolean payloadCanFulfilModel(JsonObject payload) {

        Set<String> requiredFields = Set.of(
            "code",    
            "name",
            "weight"
        );
        return payload.keySet().containsAll(requiredFields);

    } 


    @Override
    public void addObject() {

        post(BASE_ENDPOINT, "application/json", (req, resp) -> {

            JsonObject requestBody = DroneService.GSON.fromJson(req.body(), JsonObject.class);

            if(!payloadCanFulfilModel(requestBody)) {

                resp.status(400);
                return null;

            }

            Medication toCreate = new Medication();
            try { 
                
                repository.addNew(toCreate); 
                return toCreate;

            } catch(UnmetConditionsException ex) {
                resp.status(400);
                return buildResponse(ex.getMessage());
            }

        });

    }

    @Override
    public void bulkAdd() {

        post(BASE_ENDPOINT + "/bulk", PAYLOAD_ENCODING, (req, resp) -> {

            JsonObject requestBody = DroneService.GSON.fromJson(req.body(), JsonObject.class); 
            JsonArray bulkData = requestBody.get("bulk").getAsJsonArray();
            List<Medication> medicationsToCreate = new ArrayList<>();

            try { 
            bulkData.forEach((jsonMedication) -> {

                JsonObject toBuild = DroneService.GSON.fromJson(jsonMedication, JsonObject.class);                
                if(payloadCanFulfilModel(toBuild)) {

                    try {
                        medicationsToCreate.add(new Medication(
                            toBuild.get("code").getAsString(),
                            toBuild.get("name").getAsString(),
                            toBuild.get("weight").getAsInt()
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
            List<?> bulkAddResult = repository.addNewBulk(medicationsToCreate);
            return buildBulkResponse((int) bulkAddResult.get(0), (List<?>) bulkAddResult.get(1));
        } catch (SQLException ex) {
            resp.status(422);
            return buildResponse(ex.getCause().getMessage());
        }

        });

    }
    
}
