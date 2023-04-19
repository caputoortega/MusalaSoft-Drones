package ar.com.caputo.drones.database.model;

import java.lang.System.Logger.Level;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;

import ar.com.caputo.drones.DroneService;

public class BatteryAuditLog extends BaseEntityModel {

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(columnName= "drone_sn", foreign = true, canBeNull = false, foreignAutoRefresh = false)
    private Drone drone;

    @DatabaseField(canBeNull = false)
    private int loggedBatteryLevel;

    /**
     * Is this log registered during
     * the service shutdown?
     * Shutdown logs are non-automated
     * battery logs triggered AFTER a
     * shutdown command is sent.
     */
    @DatabaseField(canBeNull = true)
    private boolean shutdownLog;

    @DatabaseField(canBeNull = false, dataType = DataType.TIME_STAMP)
    private Timestamp timestamp;

    /**
     * No-args constructor required for ORMLite reflection-based mapping
     */
    public BatteryAuditLog() {}

    public BatteryAuditLog(Drone drone, boolean isShutdownLog) {
        this.drone = drone;
        this.loggedBatteryLevel = drone.getBatteryLevel();
        this.timestamp = Timestamp.valueOf(LocalDateTime.now());
        this.shutdownLog = isShutdownLog;
    }

    public int getId() {
        return id;
    }

    public Drone getDrone() {
        return drone;
    }

    public void setDrone(Drone drone) {
        this.drone = drone;
    }

    public int getLoggedBatteryLevel() {
        return loggedBatteryLevel;
    }

    public void setLoggedBatteryLevel(int loggedBatteryLevel) {
        this.loggedBatteryLevel = loggedBatteryLevel;
    }

    public LocalDateTime getTimestamp() {
        return timestamp.toLocalDateTime();
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = Timestamp.valueOf(timestamp);
    }

    public boolean isShutdownLog() {
        return this.shutdownLog;
    }

    public void setShutdownLog(boolean isShutdownLog) {
        this.shutdownLog = isShutdownLog;
    }

    @Override
    public String id() {
        return String.valueOf(getId());
    }

    /**
     * Forcefully set to false.
     * Logs should not be deleted.
     */
    @Override
    public boolean canBeDeleted() {
       return false;
    }

    /**
     * Forcefully set to true.
     * No validation takes place.
     */
    @Override
    public boolean validateId(String id) {
        return true;
    }
    
}
