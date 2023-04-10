package ar.com.caputo.drones.database.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;

/**
 * Drone mapping
 */
public class Drone extends BaseEntityModel {

    /**
     * Represents Drone model
     */
    public enum Model {
        // Placeholder value for default initialisation
        UNKNOWN,
        
        LIGHTWEIGHT,
        MIDDLEWEIGHT,
        CRUISERWEIGHT,
        HEAVYWEIGHT;
    }

    /**
     * Represents Drone state
     */
    public enum State {
        // Placeholder value for default initialisation
        UNKNOWN,

        IDLE,
        LOADING,
        LOADED,
        DELIVERING,
        DELIVERED,
        RETURNING;
    }

    @DatabaseField(id = true, columnName = "sn", columnDefinition = "VARCHAR(100) NOT NULL", useGetSet = true) 
    private String serialNumber;

    @DatabaseField(canBeNull = false, dataType = DataType.ENUM_NAME, defaultValue = "UNKNOWN")
    private Model model = Model.UNKNOWN;

    @DatabaseField(canBeNull = false, dataType = DataType.ENUM_NAME, defaultValue = "UNKNOWN")
    private State state = State.UNKNOWN; 

    @DatabaseField(canBeNull = false, defaultValue = "500")
    private int weightLimit;
    
    @DatabaseField(canBeNull = false, defaultValue = "0")
    private int batteryLevel;

    /**
     * Empty constructor required for ORMLite reflection-based mapping
     */
    public Drone() {}
    
    public String getSerialNumber() {
        return this.serialNumber;
    }

    public void setSerialNumber(String serialNumber) {

        // Making sure the serialNumber doesn't contain
        // more than 100 chars
        this.serialNumber = serialNumber.substring(0, 100);

    }

}
