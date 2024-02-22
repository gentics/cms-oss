import { DefaultModelType, ModelType, NormalizableEntity } from './type-util';

export interface ScheduleExecution<T extends ModelType = DefaultModelType> extends NormalizableEntity<T> {
    /** ID of the Execution */
    id?: number;

    /** The Schedule which started this execution */
    scheduleId: number;

    /** Timestamp when this exeuction was started */
    startTime: number;

    /** Timestamp when this execution has finished */
    endTime: number;

    /** The duration for how long the execution took */
    duration: number;

    /** If the execution was successful */
    result: boolean;

    /** The log the execution produced */
    log: string;

    /** If the execution is running */
    running: boolean;
}
