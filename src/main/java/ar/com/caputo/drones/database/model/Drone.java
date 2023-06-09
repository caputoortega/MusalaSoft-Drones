package ar.com.caputo.drones.database.model;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Set;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;

import ar.com.caputo.drones.exception.InvalidInputFormatException;
import ar.com.caputo.drones.exception.RequestProcessingException;
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

    @DatabaseField(id = true, columnName = "sn", columnDefinition = "VARCHAR(100) NOT NULL", unique = true, uniqueIndex = true, useGetSet = true) 
    private String serialNumber;

    @DatabaseField(canBeNull = false, dataType = DataType.ENUM_NAME, defaultValue = "UNKNOWN")
    private Model model = Model.UNKNOWN;

    @DatabaseField(canBeNull = false, dataType = DataType.ENUM_NAME, defaultValue = "UNKNOWN")
    private State state = State.UNKNOWN; 

    @DatabaseField(canBeNull = false, defaultValue = "500", useGetSet = true)
    private int weightLimit;
    
    @DatabaseField(canBeNull = false, defaultValue = "0", useGetSet = true)
    private int batteryLevel;

    @ForeignCollectionField(foreignFieldName = "associatedDrone", eager = false)
    private transient ForeignCollection<Medication> load;

    /**
     * No-args constructor required for ORMLite reflection-based mapping
     */
    public Drone() {
        this.ignoredUpdateAttributes = Set.of("load");
    }
    
    public Drone(String serialNumber,
                 String model, String state,
                 int weightLimit, int batteryLevel) throws InvalidInputFormatException {
        
        this.ignoredUpdateAttributes = Set.of("load");
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
        
        if(validateId(serialNumber)) this.serialNumber = serialNumber;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Drone drone = (Drone) o;
        return weightLimit == drone.weightLimit &&
                batteryLevel == drone.batteryLevel &&
                serialNumber.equals(drone.serialNumber) &&
                model == drone.model &&
                state == drone.state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(serialNumber, model, state, weightLimit, batteryLevel);
    }

    public ForeignCollection<Medication> getLoad() throws SQLException {
        return this.load;
    }
    
    /**
     *  If drone is not IDLE it means it's performing some
     *  operation, or that the current state is unknown.
     *  For said reason, and to prevent losing contact with
     *  it causing a potential incident, deletions can only
     *  be performed while their state is set to IDLE
     */
    @Override
    public boolean canBeDeleted() throws RequestProcessingException {

        if(getState() != Drone.State.IDLE)
            throw new RequestProcessingException("Cannot delete drone while not IDLE");
        return true;
    }

    public int getTotalWeight() {
        return (load.stream().mapToInt(Medication::getWeight)).sum();
    }

    /**
     * Determines if the new weight to be added can
     * be held by the drone.
     * For calculating if an upda
     * @param weight
     * @return
     */
    public boolean canHold(int weight) {
        return getTotalWeight() + weight <= weightLimit;
    }

    /**
     * Calculates the weight difference for a medication.
     * This method is used <b>when updating the weight
     * of a medication</b> and <b>NOT</b> when calculating
     * whether a new medication can be added or not. For that
     * you can use {@link #canHold}
     * The calculation performed by this method is:
     * <p>
     * <math> (Σ(l) - Wo) + Wn <= L </math>
     * </p>
     * <p>where: </p>
     * <p>l = {@link #load} individual weights</p>
     * <p>Wo = {@param medicationOldWeight}</p>
     * <p>Wn = {@param medicationNewWeight}</p>
     * <p>L = {@link #weightLimit}</p>
     * @param medicationOldWeight
     * @param medicationNewWeight
     * @return whether the weight update falls into the {@link #weightLimit} range
     */
    public boolean canHoldWeightDifference(int medicationOldWeight, int medicationNewWeight) {
        return (getTotalWeight() - medicationOldWeight) + medicationNewWeight <= weightLimit;
    }

    /**
     * Checks if the drone state is valid for load
     * and their battery level is above 25.
     * Valid states for loading items are {@link State#IDLE}
     * and {@link State#LOADING}.
     * @return whether the drone can be accept a new load loaded
     */
    public boolean canBeLoaded() {
        return (this.state == State.IDLE || this.state == State.LOADING) && batteryLevel > 25; 
    }

    /**
     * Checks if the current drone state is a valid
     * state for unloading items.
     * This states are: {@link State#IDLE}, {@link State#LOADING},
     * {@link State#LOADED} and {@link State#DELIVERED}
     * @return whether the drone state is valid to unload a medication
     */
    public boolean canBeUnloaded() {

        switch(this.state) {
            case IDLE:
            case LOADING:
            case LOADED:
            case DELIVERED:
                return true;
            default:
                return false;
        }
    }

    public boolean shouldStateBeReset() {
        return batteryLevel < 25 && (this.state == State.LOADING || this.state == State.LOADED);
    }


    @Override
    public String id() { return getSerialNumber(); }

    @Override
    public boolean validateId(String id) {

        /*
          * Making sure the serialNumber doesn't contain more than
          * 100 characters
        */
        if(id.length() > 100) throw new InvalidInputFormatException(id, "any string shorter than 100 characters");

        return true;
    }

    @Override
    public String futureId() {
        return this.futureId;
    }

}
