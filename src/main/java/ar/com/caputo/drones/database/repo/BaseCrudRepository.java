package ar.com.caputo.drones.database.repo;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.table.TableUtils;

import ar.com.caputo.drones.DroneService;
import ar.com.caputo.drones.database.model.BaseEntityModel;
import ar.com.caputo.drones.exception.RequestProcessingException;
import ar.com.caputo.drones.exception.ResourceNotFoundException;
import ar.com.caputo.drones.exception.UnmetConditionsException;

public class BaseCrudRepository<T extends BaseEntityModel, ID> {

    private final Class<T> type;

    private Dao<T, ID> dao;

    public BaseCrudRepository(Class<T> model) {
        this.type = model;
        try { 
            this.dao = DaoManager.createDao(DroneService.getInstance().getDataSource(), model);
            TableUtils.createTableIfNotExists(DroneService.getInstance().getDataSource(), model);
        
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public T get(ID id) {

        try { 
            
            T result = dao.queryForId(id);

            if(result != null) return result;
            else throw new ResourceNotFoundException(id, type);
            
        } catch (SQLException ex) {
            throw new RequestProcessingException(ex.getCause().getMessage());
        }

    }
    
    public List<T> listAll() {
    
        try {
            return dao.queryForAll();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return Collections.emptyList();
        
    }

    public boolean addNew(T model) throws UnmetConditionsException {
        
        try {
            return dao.create(model) == 1;
        } catch(SQLException ex) {
            throw new UnmetConditionsException(ex.getCause().getMessage());
        }

    }


    public List<?> addNewBulk(List<T> bulk) throws SQLException {
      return TransactionManager.callInTransaction(DroneService.getInstance().getDataSource(), new Callable<List<?>>() {

        @Override
        public List<?> call() throws Exception {
            return List.of(dao.create(bulk), bulk);
        }

      });
        
    }

    public boolean update(T model) throws SQLException {

        if(model.futureId() != null & !model.id().equals(model.futureId())) {
            return dao.update(model) == 1 && dao.updateId(model, (ID)model.futureId()) == 1;
        }

        return dao.update(model) ==1;

    }

    public boolean delete(ID id) {

        try {
            return dao.deleteById(id) == 1;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return false;

    }

    public Dao<T, ID> getDao() {
        return this.dao;
    }

    
}
