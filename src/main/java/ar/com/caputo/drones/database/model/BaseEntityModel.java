package ar.com.caputo.drones.database.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonElement;

import ar.com.caputo.drones.DroneService;
import ar.com.caputo.drones.exception.RequestProcessingException;
import ar.com.caputo.drones.exception.UnmetConditionsException;

public abstract class BaseEntityModel {

    /**
     * List of attributes to be ignored by the {@link #update()}
     * method
     */
    protected transient Set<String> ignoredUpdateAttributes;

    public BaseEntityModel() {
        this.ignoredUpdateAttributes = new HashSet<>();
    }

    public abstract String id(); 

    @Override
    public String toString() {
        return DroneService.GSON.toJson(this);
    }

    /**
     * 
     * Uses Reflection to try to perform an update
     * for a given attribute.
     * If the attribute is within the {@link #ignoredUpdateAttributes} 
     * then the procedure returns without performing
     * any further actions, else it validates whether the
     * attribute exists or not within the model and will try to
     * capture the field's type and its getter throwing an
     * {@link UnmetConditionsException} in case of an error.
     * Finally it invokes the getter with the new value
     * to update the attribute. If the invocation fails 
     * it will throw a {@link RequestProcessingException}
     * 
     * @param attribute to update
     * @param newValue  for the given attribute
     */
    public void update(String attribute, JsonElement newValue) {

        // Removes leading and trailing white spaces 
        attribute = attribute.strip();
        // Check if the attribute is set to be ignored during
        // an update
        if(ignoredUpdateAttributes.contains(attribute)) return;

        // Capitalising first letter of the attribute to 
        // complain with camelCase
        String capitalisedAttribute = attribute
                                       .substring(0,1).toUpperCase()
                                       .concat(attribute.substring(1));

        Class<?> attributeFieldType;
        Method getterMethod;

        try {

            attributeFieldType = getClass().getDeclaredField(attribute).getType();
            /*
             * Retrieving getX(Obj)
             * Since enum have two getters we are going to use the public
             * one that uses the String argument
            */
            getterMethod = getClass().getMethod("set".concat(capitalisedAttribute),
                                                attributeFieldType.isEnum() 
                                                ? String.class 
                                                : attributeFieldType);

            // Getters only have a single argument
            Type paramType = getterMethod.getGenericParameterTypes()[0];

            /*
             * For this specific implementation it is safe
             * to asume that if the argument is not an integer
             * then we can safely treat is as a String
             */
            if(paramType.getTypeName().equals("int") 
               || paramType.getTypeName().equals(Integer.class.getName()))
                getterMethod.invoke(this, newValue.getAsInt());
            else getterMethod.invoke(this, newValue.getAsString());

        } catch (NoSuchFieldException | NoSuchMethodException noSuchEx) {
            throw new UnmetConditionsException(attribute + " does not exist");
        } catch (IllegalArgumentException ex) {
            throw new UnmetConditionsException(ex.getCause().getMessage());
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new RequestProcessingException(ex.getMessage());
        }

    }
    
    public abstract boolean canBeDeleted();

}
