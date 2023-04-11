package ar.com.caputo.drones.exception;

public class UnimplementedEndpointException extends RuntimeException {

    public UnimplementedEndpointException(String method, String endpoint) {
        super(method + " endpoint " + endpoint + " has not been implemented.");
    }
    
}
