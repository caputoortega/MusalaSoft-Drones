package ar.com.caputo.drones.database.repo;

import ar.com.caputo.drones.database.model.BatteryAuditLog;

public class BatteryAuditLogRepository extends BaseCrudRepository<BatteryAuditLog, Integer> {

    public BatteryAuditLogRepository() {
        super(BatteryAuditLog.class);
    }
    
}
