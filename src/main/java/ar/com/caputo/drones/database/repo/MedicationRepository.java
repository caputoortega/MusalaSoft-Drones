package ar.com.caputo.drones.database.repo;

import com.j256.ormlite.support.ConnectionSource;

import ar.com.caputo.drones.database.model.Medication;

public class MedicationRepository extends BaseCrudRepository<Medication, String> {

    public MedicationRepository() {
        super(Medication.class);
    }
    
}
