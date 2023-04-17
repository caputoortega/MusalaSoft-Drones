package ar.com.caputo.drones.task;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import ar.com.caputo.drones.DroneService;
import ar.com.caputo.drones.database.model.BatteryAuditLog;
import ar.com.caputo.drones.database.repo.BatteryAuditLogRepository;

public class BatteryLevelLogTask {

    private BatteryAuditLogRepository repository;
    private boolean isShutdownLog = false;

    public BatteryLevelLogTask() {
        this.repository = new BatteryAuditLogRepository();
    }

    private Runnable task = () -> {

        DroneService.getInstance().getDroneEnpoint().getAllDrones().forEach(drone -> {
            repository.addNew(new BatteryAuditLog(drone, isShutdownLog));
        });

    };
    
    private ScheduledFuture<?> scheduledTask; 

    public void init() {

        this.scheduledTask = DroneService.getInstance().getScheduler()
                            .scheduleAtFixedRate(
                                this.task,
                                0,
                                DroneService.getInstance().getLogInterval(),
                                TimeUnit.SECONDS);

    }


    /**
     * Logs the levels immediately before shutting down
     * and passively cancels the scheduled task
     */
    public void shutdown() {
        this.isShutdownLog = true;
        scheduledTask.cancel(false);
/*         task.run(); */

    }
}
