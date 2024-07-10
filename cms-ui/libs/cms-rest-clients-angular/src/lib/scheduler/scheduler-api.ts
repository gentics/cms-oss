import {
    EntityIdType,
    Response,
    ScheduleCreateReqeust,
    ScheduleExecutionListOptions,
    ScheduleExecutionListResponse,
    ScheduleExecutionResponse,
    ScheduleListOptions,
    ScheduleListResponse,
    ScheduleResponse,
    SchedulerStatusResponse,
    SchedulerSuspendRequest,
    ScheduleSaveReqeust,
    ScheduleTaskCreateRequest,
    ScheduleTaskListOptions,
    ScheduleTaskListResponse,
    ScheduleTaskResponse,
    ScheduleTaskSaveRequest,
    SingleInstancePermissionType,
    SinglePermissionResponse,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';
import { stringifyEmbedOptions, stringifyPagingSortOptions } from '../util/sort-options/sort-options';

export class SchedulerApi {

    constructor(
        private apiBase: ApiBase,
    ) { }

    getStatus(): Observable<SchedulerStatusResponse> {
        return this.apiBase.get('scheduler/status');
    }

    suspend(body?: SchedulerSuspendRequest): Observable<SchedulerStatusResponse> {
        return this.apiBase.put('scheduler/suspend', body);
    }

    resume(): Observable<SchedulerStatusResponse> {
        return this.apiBase.put('scheduler/resume', null);
    }

    listTasks(options?: ScheduleTaskListOptions): Observable<ScheduleTaskListResponse> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }

        return this.apiBase.get('scheduler/task', options);
    }

    createTask(body: ScheduleTaskCreateRequest): Observable<ScheduleTaskResponse> {
        return this.apiBase.post('scheduler/task', body);
    }

    getTask(id: number): Observable<ScheduleTaskResponse> {
        return this.apiBase.get(`scheduler/task/${id}`);
    }

    getTaskPermission(id: number, permission: SingleInstancePermissionType): Observable<SinglePermissionResponse> {
        return this.apiBase.get(`perm/${permission}/160/${id}`);
    }

    updateTask(id: number, body: ScheduleTaskSaveRequest): Observable<ScheduleTaskResponse> {
        return this.apiBase.put(`scheduler/task/${id}`, body);
    }

    deleteTask(id: string | number): Observable<Response> {
        return this.apiBase.delete(`scheduler/task/${id}`);
    }

    listSchedules(options?: ScheduleListOptions): Observable<ScheduleListResponse> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }

        stringifyEmbedOptions(options);

        return this.apiBase.get('scheduler/schedule', options);
    }

    createSchedule(body: ScheduleCreateReqeust): Observable<ScheduleResponse> {
        return this.apiBase.post('scheduler/schedule', body);
    }

    getSchedule(id: number): Observable<ScheduleResponse> {
        return this.apiBase.get(`scheduler/schedule/${id}`);
    }

    getSchedulePermission(id: number, permission: SingleInstancePermissionType): Observable<SinglePermissionResponse> {
        return this.apiBase.get(`perm/${permission}/161/${id}`);
    }

    updateSchedule(id: EntityIdType, body: ScheduleSaveReqeust): Observable<ScheduleResponse> {
        return this.apiBase.put(`scheduler/schedule/${id}`, body);
    }

    deleteSchedule(id: EntityIdType): Observable<Response> {
        return this.apiBase.delete(`scheduler/schedule/${id}`);
    }

    executeSchedule(id: EntityIdType): Observable<Response> {
        return this.apiBase.post(`scheduler/schedule/${id}/execute`, {});
    }

    listExecutions(scheduleId: string | number, options?: ScheduleExecutionListOptions): Observable<ScheduleExecutionListResponse> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }

        return this.apiBase.get(`scheduler/schedule/${scheduleId}/execution`, options);
    }

    getExecution(scheduleId: number, executionId: number): Observable<ScheduleExecutionResponse> {
        return this.apiBase.get(`scheduler/execution/${executionId}`);
    }
}
