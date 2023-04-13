package ar.com.caputo.drones.exception;

public class InvalidBulkItemException extends Exception {

    public InvalidBulkItemException(String bulkJson) {
        super("An item in the bulk is not properly defined: " + bulkJson);
    }
    
}
