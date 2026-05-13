export class Message {
  uuid: string;
  notificationId: string;
  sender: string;
  recipient: string;
  recipientEmail: string;
  businessCaseId: string;
  businessCaseType: string;
  businessPartnerName: string;
  subject: string;
  date: Date;
  readAt?: Date | null;
  readBy?: string | null;
  content: string;
  metaInfo?: MetaInfoData | null; 
  files: FileData[];
  referenceNumber: string;
  language: string;
  status?: string | null;
  businessCaseTitle: string;
  settlementName: string;
  senderTenant: string;
  businessCaseNumber: string;

  constructor(data: any) {
    this.uuid = data.uuid;
    this.sender = data.sender;
    this.recipient = data.recipient;
    this.recipientEmail = data.recipientEmail;
    this.businessCaseId = data.businessCaseId;
    this.businessCaseType = data.businessCaseType;
    this.businessPartnerName = data.businessPartnerName;
    this.subject = data.subject;
    this.date = data.rawData ? new Date(data.rawData) : data.date;
    this.readAt = data.readAt ? new Date(data.readAt) : null;
    this.readBy = data.readBy || null;
    this.content = data.content;
    this.metaInfo = data.metaInfo;
    this.files = data.files;
    this.referenceNumber = data.referenceNumber;
    this.language = data.language;
    this.status = data.status || null;
    this.businessCaseTitle = data.businessCaseTitle;
    this.settlementName = data.settlementName;
    this.senderTenant = data.senderTenant;
    this.businessCaseNumber = data.businessCaseNumber;
    this.notificationId = data.notificationId;
  }

  static isValidDate(dateString: string) {
    const date = new Date(dateString);
    return !isNaN(date.getTime());
  }

  static fromJSON(data: any): Message {
    let date;
    if (typeof data.date === 'string' && this.isValidDate(data.date)) date = new Date(data.date);
    else date = data.date;
    return new Message({...data, data: data.dete});
  }

  static fromJSONArray(dataArray: any[]): Message[] {
    return dataArray.map((data) => new Message(data));
  }

  toPlainObject() {
    return { ...this }; // Returns a shallow copy as a plain object
  }

  toJSON() {
    return {
      uuid: this.uuid,
      sender: this.sender,
      recipient: this.recipient,
      recipientEmail: this.recipientEmail,
      businessCaseId: this.businessCaseId,
      businessCaseType: this.businessCaseType,
      businessPartnerName: this.businessPartnerName,
      subject: this.subject,
      date: this.date,
      readAt: this.readAt, 
      readBy: this.readBy,
      content: this.content,
      metaInfo: this.metaInfo,
      files: this.files,
      referenceNumber: this.referenceNumber,
      language: this.language,
      status: this.status,
      businessCaseTitle: this.businessCaseTitle,
      settlementName: this.settlementName,
      senderTenant: this.senderTenant,
      businessCaseNumber: this.businessCaseNumber
    };
  }
}