export class InvalidValidationResponse extends Error {
  
    constructor(message: string) {
      super(message);
      this.name = "InvalidValidationResponse";
      Object.setPrototypeOf(this, InvalidValidationResponse.prototype);
    }
  }