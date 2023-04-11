package ar.com.caputo.drones.database.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;

import ar.com.caputo.drones.exception.InvalidInputFormatException;
import ar.com.caputo.drones.exception.UnmetConditionsException;

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

    @DatabaseField(canBeNull = false, defaultValue = "500", useGetSet = true)
    private int weightLimit;
    
    @DatabaseField(canBeNull = false, defaultValue = "0", useGetSet = true)
    private int batteryLevel;

    /**
     * Empty constructor required for ORMLite reflection-based mapping
     */
    public Drone() {}
    
    public Drone(String serialNumber,
                 String model, String state,
                 int weightLimit, int batteryLevel) throws InvalidInputFormatException {
                    
        setSerialNumber(serialNumber);
        setBatteryLevel(batteryLevel);
        setWeightLimit(weightLimit);
        setModel(model);
        setState(state);

    }


    public String getSerialNumber() {
        return this.serialNumber;
    }

    /**
     * Truncates the given string to 100 chars and
     * stores it in the {@code serialNumber} attribute 
     * @param serialNumber
     */
    public void setSerialNumber(String serialNumber) {

        // Making sure the serialNumber doesn't contain
        // more than 100 chars
        this.serialNumber = serialNumber.substring(0, 100);

    }

    public int getBatteryLevel() {
        return this.batteryLevel;
    }

    /**
     * Checks whether the input is a valid integer between
     * 0 and 100 (inclusive) and updates the {@code batteryLevel}
     * attribute, else, throws an {@link InvalidInputFormatException}
     * @param level
     * @throws InvalidInputFormatException
     */
    public void setBatteryLevel(int level) throws InvalidInputFormatException {

        if(level >= 0 && level <= 100) 
            this.batteryLevel = level;
        else throw new InvalidInputFormatException(String.valueOf(level), "Integer between 0 and 100 inclusive");

    }  

    public int getWeightLimit() {
        return this.weightLimit;
    }

    /**
     * Checks whether the input is a valid integer between
     * 0 and 500 (inclusive) and updates the {@code weightLimit}
     * attribute, else, throws an {@link InvalidInputFormatException}
     * @param weightLimit
     * @throws InvalidInputFormatException
     */
    public void setWeightLimit(int weightLimit) throws InvalidInputFormatException {

        if(weightLimit >=0 && weightLimit <=500)
            this.weightLimit = weightLimit;
        else throw new InvalidInputFormatException(String.valueOf(weightLimit), "Integer between 0 and 500 inclusive");

    }

    private void setModel(Model droneModel) {
        this.model = droneModel;
    }

    public void setModel(String droneModelName) {
        setModel(Model.valueOf(droneModelName.strip().toUpperCase()));
    }

    private void setState(State droneState) {

        if(droneState == State.LOADING && this.batteryLevel < 25)
            throw new UnmetConditionsException("Cannot update drone state since battery level is lower than 25%");
        this.state = droneState;

    }

    public void setState(String droneStateName) {
        setState(State.valueOf(droneStateName.strip().toUpperCase()));
    }

    public State getState() {
        return this.state;
    }

    public Model getModel() {
        return this.model;
    }

}
