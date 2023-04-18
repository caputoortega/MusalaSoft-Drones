package ar.com.caputo.drones.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import ar.com.caputo.drones.DroneService;
import ar.com.caputo.drones.database.model.Medication;

@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
public class A02_MedicationEndpointTest extends EndpointTest {

    private final String TEST_MEDICATION_CODE = "AA01";
    private final String TEST_MEDICATION_BOX_IMAGE_URL = "https://www.roemmers.com.ar/sites/default/files/F_000001172204.png";

     /**
     * A generic test payload
     */
    private final Map<String, Object> TEST_PAYLOAD = Map.of(
        "code", TEST_MEDICATION_CODE,
        "name", "Primafenib",
        "weight", 85
    );

    private final Medication TEST_PAYLOAD_MEDICATION;


    protected A02_MedicationEndpointTest() throws Exception {
        super();
        this.TEST_PAYLOAD_MEDICATION = new Medication(
            (String)TEST_PAYLOAD.get("code"),
            (String)TEST_PAYLOAD.get("name"),
            (int)TEST_PAYLOAD.get("weight"));
    }

    @Test
    @DisplayName("GET:/medications should return an empty list")
    @Order(1)
    public void GETmedications_Should_Return_An_Empty_List() throws Exception {

        HttpResponse<String> response = client(getRequest("/medications"));

        assertEquals(200, response.statusCode(), "Invalid response code");
        assertTrue(isValidContentType(response), "Content is not JSON-encoded");

        JsonObject responseJson = DroneService.GSON.fromJson(response.body(), JsonObject.class);
        assertTrue(responseJson.getAsJsonArray("data").isEmpty(), "Response was not empty, maybe there are preloaded medications?");

    }


    @Test
    @DisplayName("POST:/medications should fail registering due to invalid code")
    @Order(2)
    public void POSTmedications_Should_Fail_Registering_Due_To_Invalid_Code() throws Exception {

        Map<String, Object> payload = new HashMap<>(TEST_PAYLOAD);
        /*
         * Lower and upper case doesn't matter since the entity
         * uppercases the value by default
        */
        payload.put("code", TEST_MEDICATION_CODE.concat("!!"));
        
        HttpResponse<String> response = client(postRequest("/medications", payload));

        assertEquals(400, response.statusCode(), "Invalid response code");

    }

    @Test
    @DisplayName("POST:/medications should fail registering due to incomplete payload")
    @Order(3)
    public void POSTmedications_Should_Fail_Registering_Due_To_Incomplete_Payload() throws Exception {

        Map<String, Object> payload = new HashMap<>();
        payload.put("code", TEST_MEDICATION_CODE);

        HttpResponse<String> response = client(postRequest("/medications", payload));
        assertEquals(400, response.statusCode(), "Invalid response code");

    }

    @Test
    @DisplayName("POST:/medications should fail registering due to invalid name")
    @Order(4)
    public void POSTmedications_Should_Fail_Registering_Due_To_Invalid_Name() throws Exception {

        Map<String, Object> payload = new HashMap<>(TEST_PAYLOAD);
        payload.put("name", "!invalidName");
        
        HttpResponse<String> response = client(postRequest("/medications", payload));

        assertEquals(400, response.statusCode(), "Invalid response code");

    }

    @Test
    @DisplayName("POST:/medications should register a new medication")
    @Order(5)
    public void POSTmedications_Should_Register_A_New_Medication() throws Exception {
        
        HttpResponse<String> response = client(postRequest("/medications", TEST_PAYLOAD));

        JsonObject responseMedicationJson = DroneService.GSON.fromJson(response.body(), JsonObject.class).getAsJsonObject("data");
        Medication addedMedication = new Medication(
            responseMedicationJson.get("code").getAsString(),
            responseMedicationJson.get("name").getAsString(),
            responseMedicationJson.get("weight").getAsInt()
        );

        assertEquals(201, response.statusCode(), "Invalid response code");
        assertEquals(TEST_PAYLOAD_MEDICATION, addedMedication, "Medications are different!");

        response = client(getRequest("/medications"));
        // Needs to be updated since this comes from a different request
        JsonArray responseJson = DroneService.GSON.fromJson(response.body(), JsonObject.class).getAsJsonArray("data");

        assertTrue(responseJson.size() == 1, "GET:/medications returned a list with more than one medication, did any of the previous silently failed?");

    }

    @Test
    @DisplayName("PATCH:/medications/<CODE> should update name")
    @Order(6)
    public void PATCHmedications_With_Code_Should_Update_Name() throws Exception {

        String newName = "Synfranil";
        Map<String, String> payload = Map.of("name",  newName); 

        HttpResponse<String> response = client(patchRequest("/medications/" + TEST_MEDICATION_CODE, payload));

        assertEquals(200, response.statusCode(), "Invalid response code");
        assertTrue(isValidContentType(response), "Content is not JSON-encoded");
        
        // Checking for updated object
        
        response = client(getRequest("/medications/" + TEST_MEDICATION_CODE));

        assertEquals(200, response.statusCode(), "Invalid response code on update check!");
        assertTrue(isValidContentType(response), "Content is not JSON-encoded on update check!");

        JsonObject responseJson = DroneService.GSON.fromJson(response.body(), JsonObject.class);

        assertEquals(newName,
                    responseJson.getAsJsonObject("data")
                        .get("name").getAsString(),
                    "Name was not updated properly!");
        

    }

    @Test
    @DisplayName("PATCH:/medications/<CODE> should fail to update image due to invalid format")
    @Order(7)
    public void PATCHmedications_With_Code_Should_Fail_To_Update_Image_Due_To_Invalid_Format() throws Exception {

        Map<String, String> payload = Map.of("medicationCaseImageUrl", "https://upload.wikimedia.org/wikipedia/commons/f/f7/Bananas.svg");

        HttpResponse<String> response = client(patchRequest("/medications/" + TEST_MEDICATION_CODE, payload));

        assertEquals(500, response.statusCode(), "Invalid response code!");
        assertTrue(isValidContentType(response), "Content is not JSON-encoded");

    }

    @Test
    @DisplayName("PATCH:/medications/<CODE> should update image")
    @Order(8)
    public void PATCHmedications_With_Code_Should_Update_Image() throws Exception {

        Map<String, String> payload = Map.of("medicationCaseImageUrl", TEST_MEDICATION_BOX_IMAGE_URL);

        HttpResponse<String> response = client(patchRequest("/medications/" + TEST_MEDICATION_CODE, payload));

        assertEquals(200, response.statusCode(), "Invalid response code!");
        assertTrue(isValidContentType(response), "Content is not JSON-encoded");

        JsonObject responseJson = DroneService.GSON.fromJson(response.body(), JsonObject.class);

        assertEquals(TEST_MEDICATION_BOX_IMAGE_URL,
                    responseJson.getAsJsonObject("data")
                        .get("medicationCaseImageUrl").getAsString(),
                    "Image was not updated properly!");

    }

    @Test
    @DisplayName("PATCH:/medications/<CODE> should update code")
    @Order(9)
    public void PATCHmedications_With_Code_Should_Update_Code() throws Exception {

        String newCode = "AB01";
        Map<String, String> payload = Map.of("code", newCode); 

        HttpResponse<String> response = client(patchRequest("/medications/" + TEST_MEDICATION_CODE, payload));

        assertEquals(200, response.statusCode(), "Invalid response code");
        assertTrue(isValidContentType(response), "Content is not JSON-encoded");
        
        // Checking for updated object
        
        response = client(getRequest("/medications"));

        JsonObject responseJson = DroneService.GSON.fromJson(response.body(), JsonObject.class);
        assertTrue(responseJson.get("data").getAsJsonArray().size() == 1, "Error on update! Did the system add a new medication instead of updating the existing one?");
       
        System.out.println(responseJson.get("data").getAsJsonArray().get(0).getAsJsonObject().get("code"));
        assertTrue(responseJson.get("data").getAsJsonArray().get(0).getAsJsonObject().get("code").getAsString().equals(newCode), "Code was not updated properly");
/* 

        response = client(getRequest("/medications/" + newCode));

        assertEquals(200, response.statusCode(), "Invalid response code on update check!");
        assertTrue(isValidContentType(response), "Content is not JSON-encoded on update check!");

        responseJson = DroneService.GSON.fromJson(response.body(), JsonObject.class);

        assertEquals(newCode,
                    responseJson.getAsJsonObject("data")
                        .get("code").getAsString(),
                    "Code was not updated properly!");         */

    }


    @Test
    @DisplayName("DELETE:/medications/<CODE> should succeed")
    @Order(10)
    public void DELETEmedications_With_Code_Should_Succeed() throws Exception {

        // Code is "AB01" because the previous test updated it!
        HttpResponse<String> response = client(deleteRequest("/medications/AB01"));

        assertEquals(200, response.statusCode(), "Invalid response code");
        assertTrue(isValidContentType(response), "Content is not JSON-encoded");

        response = client(getRequest("/medications/" + "AB01"));

        assertEquals(404, response.statusCode(), "Invalid response code on deletion check! Maybe it didn't delete the medication?");
        assertTrue(isValidContentType(response), "Content is not JSON-encoded on deletion check");
        
    }

}
