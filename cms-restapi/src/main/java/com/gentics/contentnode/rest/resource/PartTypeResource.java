package com.gentics.contentnode.rest.resource;

import com.gentics.contentnode.rest.model.PartType;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import java.util.List;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;


/**
 * Resource for part types
 */
@Path("/parttype")
public interface PartTypeResource {

  /**
   * List Part types
   *
   * @param filter filter parameters
   * @return list of part type
   * @throws Exception in case of errors
   */
  @GET
  List<PartType> list(
      @BeanParam FilterParameterBean filter) throws Exception;

}

