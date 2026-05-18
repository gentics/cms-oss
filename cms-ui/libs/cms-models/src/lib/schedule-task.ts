import { InstancePermissionItem } from './permissions';
import { DefaultModelType, ModelType, NormalizableEntity } from './type-util';

interface BaseScheduleTask<T extends ModelType = DefaultModelType> extends NormalizableEntity<T> {
    /** Creator of the Schedule-Task */
    creatorId?: number;

    /** Creation date of the Schedule-Task */
    cdate?: string;

    /** Latest Editor of the Schedule-Task */
    editorId?: number;

    /** Latest edit date of the Schedule-Task */
    edate?: string;

    /** Name of the Schedule-Task */
    name: string;

    /** Description of the Schdeule-Task */
    description?: string;

    /** The command the Task is executing */
    command: string;

    /** If this Task is an internal one */
    internal: boolean;
}

export interface ScheduleTask<T extends ModelType = DefaultModelType> extends BaseScheduleTask<T> {
    /** ID of the Schedule-Task */
    id: number;
}

/**
 * @deprecated Create your own application specific type/business object instead.
 */
export interface ScheduleTaskBO<T extends ModelType = DefaultModelType> extends BaseScheduleTask<T>, InstancePermissionItem {
    /** ID of the Schedule-Task, but as string */
    id: string;
}
