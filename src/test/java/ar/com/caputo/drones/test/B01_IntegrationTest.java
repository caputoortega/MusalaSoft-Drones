package ar.com.caputo.drones.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;

public class IntegrationTest extends EndpointTest {

     /**
     * Populates the database with drones for
     * the tests
     * @throws Exception
     */
    @BeforeAll
    public void populateDrones() throws Exception {


        Map<String, List<Map<String, Object>>> payload = new HashMap()<>();

        payload.put("bulk",
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

        client(postRequest("/drones/bulk", payload));

    }

    
}
