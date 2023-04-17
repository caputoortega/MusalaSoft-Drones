package ar.com.caputo.drones.exception;

public class InvalidBulkItemException extends RuntimeException {

    public InvalidBulkItemException(String bulkJson) {
        super("An item in the bulk is not properly defined: " + bulkJson);
    }
    
}
