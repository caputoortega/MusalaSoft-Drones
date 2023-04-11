package ar.com.caputo.drones.task;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import ar.com.caputo.drones.DroneService;

public class BatteryLevelLogTask {

    private Runnable task = () -> {

        System.out.println("LOGGING!!! " + LocalDateTime.now());
    };
    
    private ScheduledFuture<?> scheduledTask; 

    public void init() {

        this.scheduledTask = DroneService.getInstance().getScheduler()
                            .scheduleAtFixedRate(
                                this.task,
                                0,
                                5,
                                TimeUnit.SECONDS);

    }


    /**
     * Logs the levels immediately before shutting down
     * and passively cancels the scheduled task
     */
    public void shutdown() {

        scheduledTask.cancel(false);
        task.run();

    }
}
