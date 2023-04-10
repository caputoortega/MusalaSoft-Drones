package ar.com.caputo.drones.exception;

public class InvalidInputFormatException extends Exception {

    public InvalidInputFormatException(String input, String pattern) {
        super(String.format("Invalid input format: Input \"%s\" doesn't match expected pattern \"%s\"", input, pattern));
    }
    
}
