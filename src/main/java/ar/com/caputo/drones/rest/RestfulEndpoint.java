package ar.com.caputo.drones.rest;

import static spark.Spark.get;

import ar.com.caputo.drones.DroneService;
import ar.com.caputo.drones.database.repo.BaseCrudRepository;
import ar.com.caputo.drones.exception.ResourceNotFoundException;
import ar.com.caputo.drones.exception.UnimplementedEndpointException;

public class RestfulEndpoint<T> {
    
    public final String BASE_ENDPOINT;
    protected final BaseCrudRepository<T,String> repository;

    public RestfulEndpoint(final String BASE_ENDPOINT, final BaseCrudRepository<T, String> repo) {
        this.BASE_ENDPOINT = DroneService.getInstance().API_URL + BASE_ENDPOINT;
        this.repository = repo;
        register();
    
    }

    protected void register() {
        baseGet();
        getObject();
        addObject();
        updateObject();
        deleteObject();
    }


    public void baseGet() {
        get(BASE_ENDPOINT, (req, resp) -> {
            
            return repository.listAll();
        
        });
    }


    public void getObject() {
        get(BASE_ENDPOINT + "/:id", (req, resp) -> {

            try {
                return repository.get(req.params(":id"));
            } catch (ResourceNotFoundException ex) {
                resp.status(404);
                return null;
            }
        });
    }

    public void addObject() {

       throw new UnimplementedEndpointException("POST", BASE_ENDPOINT + "/add");

    }

    public void updateObject() {
        
       throw new UnimplementedEndpointException("PUT", BASE_ENDPOINT + "/:id/update");

    }

    public void deleteObject() {
        
       throw new UnimplementedEndpointException("DELETE", BASE_ENDPOINT + "/:id/delete");
       
    }

}
