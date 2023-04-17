package ar.com.caputo.drones.database.repo;

import com.j256.ormlite.support.ConnectionSource;

import ar.com.caputo.drones.database.model.Drone;

public class DroneRepository extends BaseCrudRepository<Drone, String> {

    public DroneRepository() {
        super(Drone.class);
    }
    
}
