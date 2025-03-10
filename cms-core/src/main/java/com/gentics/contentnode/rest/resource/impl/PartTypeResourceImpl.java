package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.factory.Trx.supply;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.db.DBUtils.PrepareStatement;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.model.PartType;
import com.gentics.contentnode.rest.resource.PartTypeResource;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import java.util.ArrayList;
import java.util.List;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;

@Produces({MediaType.APPLICATION_JSON})
@Authenticated
@Path("/parttype")
public class PartTypeResourceImpl implements PartTypeResource {


  @GET
  public List<PartType> list(
      @BeanParam FilterParameterBean filter) throws NodeException {
    return supply(() -> {
      PrepareStatement prepare = getPrepareStatement(filter);
      String filterQuery = prepare == null ? "" : " WHERE `deprecated` like ? OR name like ?";

      return DBUtils.select("SELECT * FROM `type`" + filterQuery, prepare, (resultSet) -> {
        List<PartType> partTypes = new ArrayList<>();

        while (resultSet.next()) {
          partTypes.add(new PartType().buildFromResultSet(resultSet));
        }
        return partTypes;
      });
    });
  }

  private PrepareStatement getPrepareStatement(FilterParameterBean filter){
    PrepareStatement prepare = null;

    if (filter != null && StringUtils.isNotEmpty(filter.query)) {
      prepare = (p) -> {
        p.setString(1, filter.query);
        p.setString(2, "%"+filter.query + "%");
      };
    }

    return prepare;
  }

}

