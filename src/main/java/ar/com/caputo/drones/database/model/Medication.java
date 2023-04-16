package ar.com.caputo.drones.database.model;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;

import com.j256.ormlite.field.DatabaseField;

import ar.com.caputo.drones.exception.InvalidInputFormatException;
import ar.com.caputo.drones.exception.RequestProcessingException;
import ar.com.caputo.drones.exception.ResourceNotFoundException;

public class Medication extends BaseEntityModel {

    /**
     * Only uppercased letters, numbers and underscore
     */
    private transient final String CODE_REGEX = "[A-Z0-9_]+";
    /**
     * Only letters (upper and lower case), numbers,
     * hyphen or underscore
     */
    private transient final String NAME_REGEX = "[A-Za-z0-9-_]+";

    /**
     * {@code code} attribute is being forced to be updated
     * and retrieved using getters and setters instead of the
     * default reflection method to check for invalid inputs.
     */
    @DatabaseField(id = true, columnDefinition = "VARCHAR(255) NOT NULL", uniqueIndex = true, unique = true, useGetSet = true)
    private String code;

    /**
     * {@code name} attribute is being forced to be updated
     * and retrieved using getters and setters instead of the
     * default reflection method to check for invalid inputs.
     */
    @DatabaseField(canBeNull = false, useGetSet = true)
    private String name;

    @DatabaseField(canBeNull = false)
    private int weight;

    @DatabaseField(canBeNull = true, useGetSet = true)
    private String medicationCaseImageUrl;

    @DatabaseField(foreign = true, canBeNull = true, foreignAutoRefresh = true)
    private transient Drone associatedDrone;

    
    /**
     * No-args constructor required for ORMLite reflection-based mapping
     */
    public Medication() {
        this.ignoredUpdateAttributes = Set.of("associatedDrone");
    }

    public Medication(String code, String name, int weight) throws InvalidInputFormatException {

        this.ignoredUpdateAttributes = Set.of("associatedDrone");
        setCode(code);
        setName(name);
        setWeight(weight);

    }

    public String getCode() {
        return code;
    }

    /**
     * Tests the input to the provided code pattern, if
     * it fits, the code on the entity gets updated, else
     * an {@link InvalidInputFormatException} is thrown 
     * @param code the code for the medication
     * @throws InvalidInputFormatException
     */
    public void setCode(String code) throws InvalidInputFormatException {

        // Uppercasing the code in case the user forgot :)
        code = code.toUpperCase();

        if(!Pattern.matches(CODE_REGEX, code)) 
            throw new InvalidInputFormatException(code, CODE_REGEX);

        this.code = code;

    }

    public String getName() {
        return this.name;
    }

    /**
     * Tests the input to the provided name pattern, if
     * it fits, the name on the entity gets updated, else
     * an {@link InvalidInputFormatException} is thrown
     * @param name the name for the medication
     * @throws InvalidInputFormatException
     */
    public void setName(String name) throws InvalidInputFormatException {
        
        if(!Pattern.matches(NAME_REGEX, name))
            throw new InvalidInputFormatException(name, NAME_REGEX);

        this.name = name;

    }

    /**
     * @return Medication box image URL
     */
    public String getMedicationCaseImageUrl() {
        return this.medicationCaseImageUrl;
    }


    /**
     * Checks whether the resource on the provided URl is a
     * valid JPEG, PNG or GIF image and updates the {@code medicationCaseImage}
     * attribute.
     * If the resource is not valid throws an {@link InvalidInputFormatException},
     * if the resource is not found or is unreadable throws a
     * {@link ResourceNotFoundException}
     * @param imageUrl 
     * @throws InvalidInputFormatException
     */
    public void setMedicationCaseImageUrl(String imageUrl) throws InvalidInputFormatException, ResourceNotFoundException {

        if(imageUrl == null || imageUrl.strip().isEmpty()) return;
        try {
            URL url = new URL(imageUrl);
            InputStream in = url.openStream();
            String imageFormat =  Imaging.getImageInfo(in, null).getFormatName();
            if(imageFormat.equals("JPEG") || imageFormat.equals("PNG") || imageFormat.equals("GIF")) 
                this.medicationCaseImageUrl = imageUrl;
            else throw new InvalidInputFormatException(imageUrl, "JPEG, PNG, GIF");
        } catch (IOException | ImageReadException ex) {
            throw new ResourceNotFoundException(imageUrl);
        }

    }

    public int getWeight() {
        return this.weight;
    }

    /**
     * Will update the medication weight only if
     * the {@link #associatedDrone} (if it has one)
     * can support the weight difference, else it
     * throws a {@link RequestProcessingException}
     * @param weight that the medication should have
     */
    public void setWeight(int weight) {

        if(associatedDrone == null ||
           associatedDrone.canHoldWeightDifference(this.weight, weight)) {
            this.weight = weight;
        } else {
           throw new RequestProcessingException("The weight difference cannot be held by the associated drone!");
        }

    }

    public Drone getAssociatedDrone() {
        return associatedDrone;
    }

    public void setAssociatedDrone(Drone associatedDrone) {
        this.associatedDrone = associatedDrone;
    }

    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Medication medication = (Medication) o;
        return weight == medication.weight &&
                code.equals(medication.code) &&
                name.equals(medication.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name, weight);
    }

    @Override
    public String id() { return getCode(); }

    /**
      * If the medication has been associated to a drone it
      * means it cannot be deleted from the database or else
      * it will cause the drones' load data to be unreliable 
     */
    @Override
    public boolean canBeDeleted() throws RequestProcessingException {

        if(getAssociatedDrone() != null)
            throw new RequestProcessingException("Cannot delete medication when it is associated to a drone load!");
        return true;
    }

    

    
}
