package ar.com.caputo.drones.exception;

/**
 * This exception is thrown whenever a lookup to the database
 * for a specific object resolves in no results.
 */
public class ResourceNotFoundException extends Exception {
    
    public ResourceNotFoundException(Object id, Class<?> objectType) {
        super(String.format("Could not find %s of ID %s", objectType.getSimpleName(), id));
    }

    public ResourceNotFoundException(String resourceUrl) {
        super("Could not find requested resource on " + resourceUrl);
    }

}
