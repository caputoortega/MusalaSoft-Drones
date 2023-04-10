package ar.com.caputo.drones.database.model;

import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;

import ar.com.caputo.drones.exception.InvalidInputFormatException;

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

    @DatabaseField(canBeNull = true, useGetSet = true, dataType = DataType.STRING_BYTES)
    private String medicationCaseImage;
    
    /**
     * Empty constructor required for ORMLite reflection-based mapping
     */
    public Medication() {}

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
     * @return Base64 string of the medication box image
     */
    public String getMedicationCaseImage() {
        return this.medicationCaseImage;
    }


    /**
     * Checks whether the string is a valid Base64 string
     * and updates the {@code medicationCaseImage} attribute
     * If the string is not a valid Base64 it throws an
     * {@link InvalidInputFormatException} 
     * @param base64String
     * @throws InvalidInputFormatException
     */
    public void setMedicationCaseImage(String base64String) throws InvalidInputFormatException {

        if(Base64.isBase64(base64String))
            this.medicationCaseImage = base64String;
        else throw new InvalidInputFormatException(base64String, "Base64");

    }

    
}
