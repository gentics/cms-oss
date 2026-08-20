export class Task {
  taskId: string;
  notificationIds: string[];
  dialogPortalUserId: string;
  dialogPortalUserame: string;
  businessPartnerId: string;
  businessPartnerName: string;
  businessCaseId: string;
  businessCaseNumber: string;
  businessCaseType: string;
  date: Date;
  titel: string;
  message: string;
  communicationForm: any;
  files: FileData[];
  taskAssigneeName: string;
  taskAssignedAt: string;
  taskAssigneeUserId: string;
  myTask: boolean;

  constructor(data: any) {
    this.taskId = data.taskId;
    this.notificationIds = data.notificationIds;
    this.dialogPortalUserId = data.dialogPortalUserId;
    this.dialogPortalUserame = data.dialogPortalUserame;
    this.businessPartnerId = data.businessPartnerId;
    this.businessPartnerName = data.businessPartnerName;
    this.businessCaseId = data.businessCaseId;
    this.businessCaseNumber = data.businessCaseNumber;
    this.businessCaseType = data.businessCaseType;
    this.date = new Date(data.rawData);
    this.communicationForm = data.communicationForm;
    this.titel = data.titel;
    this.message = data.message;
    this.files = data.files;
    this.taskAssigneeName = data.taskAssigneeName ? data.taskAssigneeName : null;
    this.taskAssignedAt = data.taskAssignedAt ? data.taskAssignedAt : null;
    this.taskAssigneeUserId = data.taskAssigneeUserId ? data.taskAssigneeUserId : null;
    this.myTask = data.myTask;
  }

  static fromJSON(data: any): Task {
    return new Task(data);
  }

  static fromJSONArray(dataArray: any[]): Task[] {
    return dataArray.map((data) => new Task(data));
  }

  toPlainObject() {
    return { ...this }; // Returns a shallow copy as a plain object
  }
}