package ar.com.caputo.drones.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;

public class B01_IntegrationTest extends EndpointTest {

     protected B01_IntegrationTest() throws Exception {
        super();
    }

    /**
     * Populates the database with drones for
     * the tests
     * @throws Exception
     */
    @BeforeAll
    public void populateDrones() throws Exception {


        Map<String, List<Map<String, Object>>> dronePayload = new HashMap<>();
        Map<String, List<Map<String, Object>>> medicationPayload = new HashMap<>();

        dronePayload.put("bulk",
            List.of(
                Map.of("serialNumber", UUID.randomUUID().toString(),
                            "model", "LIGHTWEIGHT",
                            "state", "IDLE",
                            "weightLimit", 230,
                            "batteryLevel", 43),
                Map.of("serialNumber", UUID.randomUUID().toString(), 
                            "model", "CRUISERWEIGHT",
                            "state", "LOADING",
                            "weightLimit", 470,
                            "batteryLevel", 93),
                Map.of("serialNumber", UUID.randomUUID().toString(), 
                        "model", "HEAVYWEIGHT",
                        "state", "DELIVERING",
                        "weightLimit", 400,
                        "batteryLevel",22)
            )
        );

        medicationPayload.put("bulk",
                List.of(
                        Map.of("code", "CD125",
                                "name", "CRODENU",
                                "weight", 21),
                        Map.of("code", "BXN1000",
                                "name", "BEXRANON",
                                "weight", 73),
                        Map.of("code", "ADS500",
                                "name", "ALDACSONE",
                                "weight", 46)
                       )
        );

        client(postRequest("/drones/bulk", dronePayload));
        client(postRequest("/medications/bulk", medicationPayload));

    }

    
}
