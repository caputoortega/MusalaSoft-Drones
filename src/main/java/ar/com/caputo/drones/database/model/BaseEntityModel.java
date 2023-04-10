package ar.com.caputo.drones.database.model;

import ar.com.caputo.drones.DroneService;

public class BaseEntityModel {

    @Override
    public String toString() {
        return DroneService.GSON.toJson(this);
    }
    
}
