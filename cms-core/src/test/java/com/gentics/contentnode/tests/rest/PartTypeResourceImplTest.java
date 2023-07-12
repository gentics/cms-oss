package com.gentics.contentnode.tests.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.rest.model.PartType;
import com.gentics.contentnode.rest.resource.impl.PartTypeResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.testutils.DBTestContext;
import java.util.List;
import org.junit.ClassRule;
import org.junit.Test;


public class PartTypeResourceImplTest {

  @ClassRule
  public static DBTestContext testContext = new DBTestContext();

  @Test
  public void givenPartTypeRequest_shouldReturnItems() throws NodeException {
    List<PartType> partTypes = new PartTypeResourceImpl().list(null);

    assertThat(partTypes).isNotEmpty();
    assertThat(partTypes.get(0)).hasFieldOrProperty("id");
    assertThat(partTypes.get(0)).hasFieldOrProperty("name");
    assertThat(partTypes.get(0)).hasFieldOrProperty("deprecated");
    assertThat(partTypes.get(0)).hasFieldOrProperty("javaClass");
  }

  @Test
  public void givenFilteredRequest_shouldContainOnlyDeprecatedItems() throws NodeException {
    Trx.operate(() -> DBUtils.update("UPDATE `type` SET `deprecated` = ? where id = ?", 1, 9));

    List<PartType> partTypes = new PartTypeResourceImpl().list(
        new FilterParameterBean().setQuery("1"));

    assertThat(partTypes).isNotEmpty();
    assertThat(partTypes.get(0)).hasFieldOrPropertyWithValue("deprecated",  true);
  }


}