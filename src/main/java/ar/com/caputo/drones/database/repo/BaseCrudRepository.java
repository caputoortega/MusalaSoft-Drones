package ar.com.caputo.drones.database.repo;

import java.lang.reflect.ParameterizedType;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;


import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import ar.com.caputo.drones.DroneService;
import ar.com.caputo.drones.exception.ResourceNotFoundException;

public class BaseCrudRepository<T, I> {

    private final Class<T> type;

    protected Dao<T, I> dao;

    public BaseCrudRepository(ConnectionSource source, Class<T> model) {
        this.type = model;
        try { 
            this.dao = DaoManager.createDao(DroneService.getInstance().getDataSource(), model);
            TableUtils.createTableIfNotExists(DroneService.getInstance().getDataSource(), model);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public T get(I id) throws ResourceNotFoundException {

        try { 
            
            T result = dao.queryForId(id);

            if(result != null) return result;
            else throw new ResourceNotFoundException(id, type);
            
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return null;

    }
    
    public List<T> listAll() {
    
        try {
            return dao.queryForAll();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return Collections.emptyList();
        
    }

    public boolean addNew(T model) {
        
        try {
            return dao.create(model) == 1;
        } catch(SQLException ex) {
            ex.printStackTrace();
        }

        return false;

    }

    public boolean update(T model) {

        try {
            return dao.update(model) == 1;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return false;

    }

    public boolean delete(I id) {

        try {
            return dao.deleteById(id) == 1;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return false;

    }
    
}
