export enum SchedulerStatus {
    /** Scheduler is running */
    RUNNING = 'running',
    /** Scheduler is suspending (jobs are still running) */
    SUSPENDING = 'suspending',
    /** Scheduler is suspended */
    SUSPENDED = 'suspended',
}
