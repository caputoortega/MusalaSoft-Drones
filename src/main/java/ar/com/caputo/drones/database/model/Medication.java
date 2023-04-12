package ar.com.caputo.drones.database.model;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.regex.Pattern;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;

import com.j256.ormlite.field.DatabaseField;

import ar.com.caputo.drones.exception.InvalidInputFormatException;
import ar.com.caputo.drones.exception.ResourceNotFoundException;

public class Medication extends BaseEntityModel {

    /**
     * Only uppercased letters, numbers and underscore
     */
    private final String CODE_REGEX = "/[A-Z0-9_]/g";
    /**
     * Only letters (upper and lower case), numbers,
     * hyphen or underscore
     */
    private final String NAME_REGEX = "/[A-Za-z0-9-_]/g";

    /**
     * {@code code} attribute is being forced to be updated
     * and retrieved using getters and setters instead of the
     * default reflection method to check for invalid inputs.
     */
    @DatabaseField(id = true, columnDefinition = "VARCHAR(255) NOT NULL", useGetSet = true)
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

    @DatabaseField(foreign = true, canBeNull = true)
    private Drone associatedDrone;
    
    /**
     * Empty constructor required for ORMLite reflection-based mapping
     */
    public Medication() {}

    public Medication(String code, String name, int weight) throws InvalidInputFormatException {

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
     * @param base64String
     * @throws InvalidInputFormatException
     */
    public void setMedicationCaseImageUrl(String imageUrl) throws InvalidInputFormatException, ResourceNotFoundException {

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

    public void setWeight(int weight) {
        this.weight = weight;
    }

    
}
