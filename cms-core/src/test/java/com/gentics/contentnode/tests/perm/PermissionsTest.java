package com.gentics.contentnode.tests.perm;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.perm.Permissions;
import com.gentics.lib.etc.StringUtils;

/**
 * Tests for getting permissions
 */
public class PermissionsTest {
	/**
	 * Test getting empty permissions
	 * @throws Exception
	 */
	@Test
	public void testGetEmptyPermission() throws Exception {
		// empty permissions should be null
		assertNull("Empty permissions should be null", Permissions.get(StringUtils.repeat("0", 32)));
	}

	/**
	 * Test getting illegal permissions
	 * @throws Exception
	 */
	@Test
	public void testGetIllegalPermission() throws Exception {
		// getting illegal permissions must throw an exception
		for (String perms : Arrays.asList(StringUtils.repeat("1", 31), StringUtils.repeat("1", 33), "Illegal")) {
			try {
				Permissions.get(perms);
				fail("Illegal permission " + perms + " failed to throw an exception");
			} catch (NodeException expected) {
				// this exception is expected
			}
		}
	}

	/**
	 * Test getting the same permission twice
	 * @throws Exception
	 */
	@Test
	public void testGetSamePermission() throws Exception {
		// test whether getting the same permissions twice will return the same object again
		Permissions first = Permissions.get(StringUtils.repeat("10", 16));
		Permissions second = Permissions.get(StringUtils.repeat("10", 16));
		assertTrue("Got different objects for the same permission bits", first == second);
	}

	/**
	 * Test getting a permission
	 * @throws Exception
	 */
	@Test
	public void testGetPermission() throws Exception {
		for (int i = 0; i < 100; i++) {
			String permBits = getRandomPermissionBits();
			Permissions perm = Permissions.get(permBits);
			assertEquals("Check generated permissions object", permBits, perm.toString());
		}
	}

	/**
	 * Test merging permissions
	 * @throws Exception
	 */
	@Test
	public void testMergePermissions() throws Exception {
		List<Permissions> perms = new ArrayList<Permissions>();
		for (int i = 0; i < 32; i++) {
			Permissions perm = Permissions.get(StringUtils.repeat("0", i) + "1" + StringUtils.repeat("0", 32 - 1 - i));
			perms.add(perm);
			Permissions merged = Permissions.merge(perms);

			assertEquals("Check merged permissions", StringUtils.repeat("1", i + 1) + StringUtils.repeat("0", 32 - i - 1), merged.toString());
		}
	}

	/**
	 * Test checking permission bits
	 * @throws Exception
	 */
	@Test
	public void testCheckPermissionBits() throws Exception {
		for (int i = 0; i < 100; i++) {
			String permBits = getRandomPermissionBits();
			Permissions perm = Permissions.get(permBits);
			for (int bit = 0; bit < 32; bit++) {
				assertEquals("Check permbit " + bit + " for " + permBits, permBits.charAt(bit) == '1', perm.check(bit));
			}
		}
	}

	/**
	 * Test changing permissions with patterns
	 * @throws NodeException
	 */
	@Test
	public void testChangeWithPattern() throws NodeException {
		Permissions perm = Permissions.get("11111111111111110000000000000000");
		assertThat(Permissions.change(perm, "0...............................").toString()).isEqualTo("01111111111111110000000000000000");
		assertThat(Permissions.change(perm, "...............................1").toString()).isEqualTo("11111111111111110000000000000001");
		assertThat(Permissions.change(perm, "0011").toString()).isEqualTo("00111111111111110000000000000000");
		assertThat(Permissions.change(perm, "0000000000000000")).isNull();
	}

	/**
	 * Get a random (but legal) permission bits string
	 * @return random permission bits string
	 */
	protected String getRandomPermissionBits() {
		// generate a random sequence of 1 and 0 (32 bits).
		// make sure that at least one 1 is added
		StringBuilder str = new StringBuilder();
		Random rand = new Random();
		boolean oneAdded = false;
		for (int i = 0; i < 32; i++) {
			int number = 0;
			if (i == 31 && !oneAdded) {
				// the last bit must be one, if no other was one
				number = 1;
			} else {
				number = rand.nextInt(2);
			}
			oneAdded |= number == 1;
			str.append(number);
		}

		return str.toString();
	}
}
