package ar.com.caputo.drones.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.google.gson.JsonObject;

import ar.com.caputo.drones.DroneService;
import ar.com.caputo.drones.database.model.Drone;
import ar.com.caputo.drones.exception.InvalidInputFormatException;

@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
public class DroneEndpointTest extends EndpointTest {


    /**
     * A random UUID to be used as a serial number
     */
    private final String TEST_DRONE_SERIAL = UUID.randomUUID().toString(); 
    
    /**
     * A generic test payload
     */
    private final Map<String, Object> TEST_PAYLOAD = Map.of(
        "serialNumber", TEST_DRONE_SERIAL,
        "model", Drone.Model.CRUISERWEIGHT.name(),
        "state", Drone.State.LOADED.name(),
        "weightLimit", 360,
        "batteryLevel", 89
    );

    /**
     * The drone object result of the generic test
     * payload if correctly added/retrieved
     */
    private final Drone TEST_PAYLOAD_DRONE;

    /**
     * Builds the {@link #TEST_PAYLOAD_DRONE} object for later
     * use
     * @throws InvalidInputFormatException
     */
    public DroneEndpointTest() throws Exception {
        super();
        
        this.TEST_PAYLOAD_DRONE = new Drone(
                (String)TEST_PAYLOAD.get("serialNumber"),
                (String)TEST_PAYLOAD.get("model"),
                (String)TEST_PAYLOAD.get("state"),
                (int)TEST_PAYLOAD.get("weightLimit"),
                (int)TEST_PAYLOAD.get("batteryLevel")
            );

    }

    @Test
    @DisplayName("GET:/drones should return an empty list")
    @Order(1)
    public void GETdrones_Should_Return_An_Empty_List() throws Exception {

        HttpResponse<String> response = client(getRequest("/drones"));

        assertEquals(200, response.statusCode(), "Invalid response code");
        assertTrue(isValidContentType(response), "Content is not JSON-encoded");

        JsonObject responseJson = DroneService.GSON.fromJson(response.body(), JsonObject.class);
        assertTrue(responseJson.getAsJsonArray("data").isEmpty(), "Response was not empty, maybe there are preloaded drones?");

    }

    @Test
    @DisplayName("POST:/drones should fail registering a drone incomplete payload")
    @Order(2)
    public void POSTdrones_Should_Fail_Registering_A_Drone_With_Incomplete_Payload() throws Exception {

        Map<String, Object> payload = new HashMap<>();
        payload.put("serialNumber", TEST_DRONE_SERIAL);

        HttpResponse<String> response = client(postRequest("/drones", payload));
        assertEquals(400, response.statusCode(), "Invalid response code");

    }

    @Test
    @DisplayName("POST:/drones should fail registering a drone with invalid model")
    @Order(3)
    public void POSTdrones_Should_Fail_Registering_A_Drone_With_Invalid_Model() throws Exception {

        Map<String, Object> payload = new HashMap<>(TEST_PAYLOAD);
        payload.put("model", "INVALID_MODEL_NAME_TEST");
        
        HttpResponse<String> response = client(postRequest("/drones", payload));

        assertEquals(400, response.statusCode(), "Invalid response code");

    }

    @Test
    @DisplayName("POST:/drones should fail registering a drone with invalid state")
    @Order(4)
    public void POSTdrones_Should_Fail_Registering_A_Drone_With_Invalid_State() throws Exception {
        
        Map<String, Object> payload = new HashMap<>(TEST_PAYLOAD);
        payload.put("state", "INVALID_STATE_TEST");
        
        HttpResponse<String> response = client(postRequest("/drones", payload));

        assertEquals(400, response.statusCode(), "Invalid response code");

    }

    @Test
    @DisplayName("POST:/drones should fail registering a drone with invalid weight limit (under 0)")
    @Order(5)
    public void POSTdrones_Should_Fail_Registering_A_Drone_With_Invalid_Weight_Limit_Under_0() throws Exception {
  
        Map<String, Object> payload = new HashMap<>(TEST_PAYLOAD);
        payload.put("weightLimit", -20);

        HttpResponse<String> response = client(postRequest("/drones", payload));

        assertEquals(400, response.statusCode(), "Invalid response code");

    }

    @Test
    @DisplayName("POST:/drones should fail registering a drone with invalid weight limit (over 500)")
    @Order(6)
    public void POSTdrones_Should_Fail_Registering_A_Drone_With_Invalid_Weight_Limit_Over_500() throws Exception {

        Map<String, Object> payload = new HashMap<>(TEST_PAYLOAD);
        payload.put("weightLimit", 670);

        HttpResponse<String> response = client(postRequest("/drones", payload));

        assertEquals(400, response.statusCode(), "Invalid response code");

    }

    @Test
    @DisplayName("POST:/drones should fail registering a drone with invalid battery level (under 0)")
    @Order(7)
    public void POSTdrones_Should_Fail_Registering_A_Drone_With_Invalid_Battery_Level_Under_0() throws Exception {
        
        Map<String, Object> payload = new HashMap<>(TEST_PAYLOAD);
        payload.put("batteryLevel", -15);

        HttpResponse<String> response = client(postRequest("/drones", payload));

        assertEquals(400, response.statusCode(), "Invalid response code");

    }


    @Test
    @DisplayName("POST:/drones should fail registering a drone with invalid battery level (over 100)")
    @Order(8)
    public void POSTdrones_Should_Fail_Registering_A_Drone_With_Invalid_Battery_Level_Over_100() throws Exception {
        
        Map<String, Object> payload = new HashMap<>(TEST_PAYLOAD);
        payload.put("batteryLevel", 150);

        HttpResponse<String> response = client(postRequest("/drones", payload));

        assertEquals(400, response.statusCode(), "Invalid response code");

    }

    @Test
    @DisplayName("POST:/drones should register a new drone")
    @Order(9)
    public void POSTdrones_Should_Register_A_New_Drone() throws Exception {
        
        HttpResponse<String> response = client(postRequest("/drones", TEST_PAYLOAD));

        JsonObject responseDroneJson = DroneService.GSON.fromJson(response.body(), JsonObject.class).getAsJsonObject("data");
        Drone addedDrone = new Drone(
            responseDroneJson.get("serialNumber").getAsString(),
            responseDroneJson.get("model").getAsString(),
            responseDroneJson.get("state").getAsString(),
            responseDroneJson.get("weightLimit").getAsInt(),
            responseDroneJson.get("batteryLevel").getAsInt()
        );

        assertEquals(201, response.statusCode(), "Invalid response code");
        assertEquals(TEST_PAYLOAD_DRONE, addedDrone, "Drones are different!");

    }

    @Test
    @DisplayName("POST:/drones should fail to register a drone with an already existing serial")
    @Order(10)
    public void POSTdrones_Should_Fail_Registering_A_Drone_With_An_Already_Existing_Serial() throws Exception {
    
        Map<String, Object> payload = new HashMap<>();

        payload.put("serialNumber", TEST_DRONE_SERIAL);
        payload.put("model", Drone.Model.LIGHTWEIGHT.name());
        payload.put("state", Drone.State.IDLE.name());
        payload.put("weightLimit", 244);
        payload.put("batteryLevel", 76);
        
        HttpResponse<String> response = client(postRequest("/drones", payload));

        assertEquals(400, response.statusCode(), "Invalid response code");

    }

    @Test
    @DisplayName("GET:/drones should return a non-empty list")
    @Order(11)
    public void GETdrones_Should_Return_A_Non_Empty_List() throws Exception {

        HttpResponse<String> response = client(getRequest("/drones"));

        assertEquals(200, response.statusCode(), "Invalid response code");
        assertTrue(isValidContentType(response), "Content is not JSON-encoded");

        JsonObject responseJson = DroneService.GSON.fromJson(response.body(), JsonObject.class);
        assertFalse(responseJson.getAsJsonArray("data").isEmpty(), "Response was empty, maybe no drones were registered?");
       
    }

    @Test
    @DisplayName("GET:/drones/<SERIAL> should return the drone registered by previous tests")
    @Order(12)
    public void GETdrones_With_Serial_Should_Return_The_Previously_Registered_Drone() throws Exception {

        HttpResponse<String> response = client(getRequest("/drones/" + TEST_DRONE_SERIAL));

        assertEquals(200, response.statusCode(), "Invalid response code");
        assertTrue(isValidContentType(response), "Content is not JSON-encoded");

        JsonObject responseJson = DroneService.GSON.fromJson(response.body(), JsonObject.class).getAsJsonObject("data");

        Drone retrievedDrone = new Drone(
                                responseJson.get("serialNumber").getAsString(),
                                responseJson.get("model").getAsString(),
                                responseJson.get("state").getAsString(),
                                responseJson.get("weightLimit").getAsInt(),
                                responseJson.get("batteryLevel").getAsInt());

        assertEquals(TEST_PAYLOAD_DRONE, retrievedDrone, "Drones have different attributes!");

    }

    @Test
    @DisplayName("GET:/drones/<SERIAL>/battery should return the drone registered by previous tests battery level")
    @Order(13)
    public void GETdrones_With_Serial_Should_Return_The_Previously_Registered_Drone_BatteryLevel() throws Exception {

        HttpResponse<String> response = client(getRequest("/drones/" + TEST_DRONE_SERIAL + "/battery"));

        assertEquals(200, response.statusCode(), "Invalid response code");
        assertTrue(isValidContentType(response), "Content is not JSON-encoded");

        JsonObject responseJson = DroneService.GSON.fromJson(response.body(), JsonObject.class);

        assertEquals(TEST_PAYLOAD.get("batteryLevel"), 
                    responseJson.get("data").getAsInt(),
                    "Drones have different battery levels! Are they the same?");

    }

    @Test
    @DisplayName("DELETE:/drones/<SERIAL> should fail on non idle state")
    @Order(14)
    public void DELETEdrones_With_Serial_Should_Fail_On_Non_Idle_State() throws Exception {

        HttpResponse<String> response = client(deleteRequest("/drones/" + TEST_DRONE_SERIAL));

        assertEquals(409, response.statusCode(), "Invalid response code");
        assertTrue(isValidContentType(response), "Content is not JSON-encoded");
        
    }

    @Test
    @DisplayName("PATCH:/drones/<SERIAL> should update state from LOADED to IDLE")
    @Order(15)
    public void PATCHdrones_With_Serial_Should_Update_State_From_LOADED_To_IDLE() throws Exception {

        Map<String, String> payload = Map.of("state", Drone.State.IDLE.name()); 

        HttpResponse<String> response = client(patchRequest("/drones/" + TEST_DRONE_SERIAL, payload));

        assertEquals(200, response.statusCode(), "Invalid response code");
        assertTrue(isValidContentType(response), "Content is not JSON-encoded");
        
        // Checking for updated object
        
        response = client(getRequest("/drones/" + TEST_DRONE_SERIAL));

        assertEquals(200, response.statusCode(), "Invalid response code on update check!");
        assertTrue(isValidContentType(response), "Content is not JSON-encoded on update check!");

        JsonObject responseJson = DroneService.GSON.fromJson(response.body(), JsonObject.class);

        assertEquals(Drone.State.IDLE.name(),
                    responseJson.getAsJsonObject("data")
                        .get("state").getAsString(),
                    "State not updated properly!");
        

    }

    @Test
    @DisplayName("POST:/drones/bulk should fail registering drones due to incomplete payload")
    @Order(16)
    public void POSTdrones_Bulk_Should_Fail_Registering_With_Incomplete_Payload() throws Exception {
    
        Map<String, List<Map<String, Object>>> payload = new HashMap<>();

        payload.put("bulk",
            List.of(
                Map.of("serialNumber", UUID.randomUUID().toString(),
                            "model", "LIGHTWEIGHT",
                            "state", "LOADING",
                            "weightLimit", 400,
                            "batteryLevel", 43),
                Map.of("serialNumber", UUID.randomUUID().toString(), 
                            "model", "LIGHTWEIGHT",
                            "state", "LOADING",
                            "batteryLevel", 43) // Missing weightLimit
            )
        );


        HttpResponse<String> response = client(postRequest("/drones/bulk", payload));

        assertEquals(400, response.statusCode(), "Invalid response code");
        assertTrue(isValidContentType(response), "Content is not JSON-encoded on update check!");


    }

    @Test
    @DisplayName("POST:/drones/bulk should register three new drones")
    @Order(17)
    public void POSTdrones_Bulk_Should_Register_Three_New_Drones() throws Exception {

        Map<String, List<Map<String, Object>>> payload = new HashMap<>();

        payload.put("bulk",
            List.of(
                Map.of("serialNumber", UUID.randomUUID().toString(),
                            "model", "LIGHTWEIGHT",
                            "state", "LOADING",
                            "weightLimit", 400,
                            "batteryLevel", 43),
                Map.of("serialNumber", UUID.randomUUID().toString(), 
                            "model", "LIGHTWEIGHT",
                            "state", "IDLE",
                            "weightLimit", 473,
                            "batteryLevel", 43),
                Map.of("serialNumber", UUID.randomUUID().toString(), 
                        "model", "CRUISERWEIGHT",
                        "state", "IDLE",
                        "weightLimit", 320,
                        "batteryLevel",22)
            )
        );


        HttpResponse<String> response = client(postRequest("/drones/bulk", payload));

        assertEquals(200, response.statusCode(), "Invalid response code");
        assertTrue(isValidContentType(response), "Content is not JSON-encoded on update check!");

        JsonObject responseJson = DroneService.GSON.fromJson(response.body(), JsonObject.class);

        assertEquals(3, responseJson.get("bulkSize").getAsInt(), "Invalid registration bulk count");

    }
    
}
