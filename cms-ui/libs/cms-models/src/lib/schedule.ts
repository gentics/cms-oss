import { InstancePermissionItem } from './permissions';
import { ScheduleExecution } from './schedule-execution';
import { DefaultModelType, ModelType, NormalizableEntity } from './type-util';

export enum ScheduleStatus {
    IDLE = 'IDLE',
    DUE = 'DUE',
    RUNNING = 'RUNNING',
}

interface BaseSchedule<T extends ModelType = DefaultModelType> extends NormalizableEntity<T> {
    /** Creator of the Schedule */
    creatorId?: number;

    /** Creation date of the Schedule */
    cdate?: string;

    /** Latest Editor of the Schedule */
    editorId?: number;

    /** Latest edit date of the Schedule */
    edate?: string;

    /** The name of the Schedule */
    name: string;

    /** Description for the Schedule */
    description?: string;

    /** The Task ID to execute */
    taskId: number;

    status: ScheduleStatus;

    /** Data which controls how the schedule is supposed to run */
    scheduleData: ScheduleData;

    /** If this Schedule is active */
    active: boolean;

    /** If this Schedule can run in parallel to other Schedules */
    parallel: boolean;

    /** A list of emails which will be notified when the Schedule experiences an error */
    notificationEmail?: string[];

    /** How many runs the Schedule has had */
    runs?: number;

    /** What the average execution/completion time of the Task in this Schedule is */
    averageTime?: number;

    /** The latest execution (to check for logs and status) */
    lastExecution?: ScheduleExecution<T>;
}

export interface Schedule<T extends ModelType = DefaultModelType> extends BaseSchedule<T> {
    /** ID of the Schedule */
    id: number;
}

/**
 * @deprecated Create your own application specific type/business object instead.
 */
export interface ScheduleBO<T extends ModelType = DefaultModelType> extends BaseSchedule<T>, InstancePermissionItem {
    /** ID of the Schedule, but as string */
    id: string;
}

export type ScheduleData = SimpleScheduleData | IntervalScheduleData | FollowUpScheduleData;

interface BaseScheduleData {
    /** The type of the Schedule */
    type: ScheduleType;

    /** When the schedule should be effective from */
    startTimestamp?: number;

    /** Until the schedule schould be effective to */
    endTimestamp?: number;
}

export interface SimpleScheduleData extends BaseScheduleData {
    type: ScheduleType.MANUAL | ScheduleType.ONCE;
}

export interface IntervalScheduleData extends BaseScheduleData {
    type: ScheduleType.INTERVAL;
    interval: ScheduleInterval;
}

export interface FollowUpScheduleData extends BaseScheduleData {
    type: ScheduleType.FOLLOW_UP;
    follow: ScheduleFollow;
}

export enum ScheduleType {
    ONCE = 'once',
    INTERVAL = 'interval',
    FOLLOW_UP = 'followup',
    MANUAL = 'manual',
}

export interface ScheduleInterval {
    /** How much of the unit the Schedule has to wait after each execution (execution start) */
    value: number;

    /** The unit of how big the value is supposed to be */
    unit: IntervalUnit;
}

export interface ScheduleFollow {
    /** The IDs after which the associated schedule should be executed.  */
    scheduleId: number[];

    /**
     * Whether this follow-up execution should only occur if the schedules in
     * `scheduleId` executed successfully.
     */
    onlyAfterSuccess: boolean;
}

export enum IntervalUnit {
    MINUTE = 'minute',
    HOUR = 'hour',
    DAY = 'day',
    WEEK = 'week',
    MONTH = 'month',
}

