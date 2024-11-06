package com.gentics.contentnode.tests.nodeobject;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for filtered property substitution of contentrepository settings
 */
@RunWith(Parameterized.class)
public class CrPropertySubstitutionTest {
	protected final static String PROPERTY_VALUE = "substituted value";

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	private static ContentRepository cr;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		context.getContext().getTransaction().commit();

		cr = create(ContentRepository.class, cr -> {
			cr.setCrType(Type.cr);
			cr.setName("Test CR");
		}).build();
	}

	@Parameters(name = "{index}: property {0}, url {1}, username {2}, password {3}, basepath {4}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		data.add(new Object[] { "INVALID_PROPERTY", false, false, false, false });
		data.add(new Object[] { "NODE_DB_PASSWORD", false, false, false, false });
		data.add(new Object[] { "CR_URL_TEST", true, false, false, false });
		data.add(new Object[] { "CR_USERNAME_TEST", false, true, false, false });
		data.add(new Object[] { "CR_PASSWORD_TEST", false, false, true, false });
		data.add(new Object[] { "CR_ATTRIBUTEPATH_TEST", false, false, false, true });
		return data;
	}

	@Parameter(0)
	public String propertyName;

	@Parameter(1)
	public boolean validForUrl;

	@Parameter(2)
	public boolean validForUsername;

	@Parameter(3)
	public boolean validForPassword;

	@Parameter(4)
	public boolean validForBasepath;

	protected String valueToSet;

	@Before
	public void setup() {
		System.setProperty(propertyName, PROPERTY_VALUE);
		valueToSet = String.format("${sys:%s}", propertyName);
	}

	@Test
	public void testUrl() throws NodeException {
		cr = update(cr, update -> {
			update.setUrlProperty(valueToSet);
		}).build();

		assertThat(execute(ContentRepository::getEffectiveUrl, cr)).as("Effective URL")
				.isEqualTo(validForUrl ? PROPERTY_VALUE : valueToSet);
	}

	@Test
	public void testUsername() throws NodeException {
		cr = update(cr, update -> {
			update.setUsernameProperty(valueToSet);
		}).build();

		assertThat(execute(ContentRepository::getEffectiveUsername, cr)).as("Effective Username")
				.isEqualTo(validForUsername ? PROPERTY_VALUE : valueToSet);
	}

	@Test
	public void testPassword() throws NodeException {
		cr = update(cr, update -> {
			update.setPassword(valueToSet);
			update.setPasswordProperty(true);
		}).build();

		assertThat(execute(ContentRepository::getEffectivePassword, cr)).as("Effective Password")
				.isEqualTo(validForPassword ? PROPERTY_VALUE : valueToSet);

		cr = update(cr, update -> {
			update.setPassword(valueToSet);
			update.setPasswordProperty(false);
		}).build();

		assertThat(execute(ContentRepository::getEffectivePassword, cr)).as("Effective Password").isEqualTo(valueToSet);
	}

	@Test
	public void testBasepath() throws NodeException {
		cr = update(cr, update -> {
			update.setBasepathProperty(valueToSet);
		}).build();

		assertThat(execute(ContentRepository::getEffectiveBasepath, cr)).as("Effective Basepath")
				.isEqualTo(validForBasepath ? PROPERTY_VALUE : valueToSet);
	}
}
