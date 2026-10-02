package com.gentics.contentnode.tests.migration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.migration.MigrationPartMapper;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.log.NodeLogger;

public class TagTypeMigrationPartMapperTest {
	
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
	}

	private static final NodeLogger logger = NodeLogger.getNodeLogger(TagTypeMigrationPartMapperTest.class);

	@Test
	public void testMapping() throws Exception {

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			Construct fromConstruct = (Construct) t.getObject(Construct.class, 19);
			Construct toConstruct = (Construct) t.getObject(Construct.class, 19);
			Map<Part, List<Part>> possibleMappings = MigrationPartMapper.getPossiblePartTypeMappings(fromConstruct, toConstruct);

			for (Part part : possibleMappings.keySet()) {
				logger.debug("Part: " + part.getName() + " " + part.getPartTypeId());

				List<Part> possibleParts = possibleMappings.get(part);

				assertTrue("There should be at least one matching part.", possibleMappings.size() > 0);
				boolean foundAtLeastOneExactMatch = false;

				for (Part possiblePart : possibleParts) {
					logger.debug(" mappable to:  " + possiblePart.getName() + " " + possiblePart.getPartTypeId());
					if (possiblePart.getPartTypeId() == part.getPartTypeId()) {
						foundAtLeastOneExactMatch = true;
					}
				}
				// For velocity parts
				if (part.getPartTypeId() == 33) {
					assertFalse("No matching parts should be found for parts with no value", foundAtLeastOneExactMatch);
					assertTrue("No possible parts should be listed for parts with no value.", possibleParts.size() == 0);
				} else {
					assertTrue("At least one part should be an exact match", foundAtLeastOneExactMatch);
				}
			}
		}
	}
}
