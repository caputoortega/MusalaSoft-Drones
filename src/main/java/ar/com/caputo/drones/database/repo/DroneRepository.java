package ar.com.caputo.drones.database.repo;

import java.sql.SQLException;

import ar.com.caputo.drones.database.model.Drone;
import ar.com.caputo.drones.exception.RequestProcessingException;

public class DroneRepository extends BaseCrudRepository<Drone, String> {

    public DroneRepository() {
        super(Drone.class);
    }

    public boolean resetState(Drone drone) {
        try { 
            drone.setState(Drone.State.IDLE.name());
            return update(drone);
        } catch(SQLException ex) {
            throw new RequestProcessingException(ex.getMessage());
        }
    }
    
}
