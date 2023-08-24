package com.gentics.contentnode.exception;

import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import javax.ws.rs.core.Response.Status;

public class InvalidRequestException extends RestMappedException{
  /**
   * Serial Version UID
   */
  private static final long serialVersionUID = -7339815097265162857L;


  public InvalidRequestException(String message) {
    super(message);
    setMessageType(Type.CRITICAL);
    setResponseCode(ResponseCode.FAILURE);
    setStatus(Status.BAD_REQUEST);
  }
}
