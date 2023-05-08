package com.gentics.contentnode.tests.publish.cr.fragment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.object.cr.CrFragmentEntry;
import com.gentics.contentnode.rest.model.ContentRepositoryFragmentEntryModel;

/**
 * Utilities for CrFragment Tests
 */
public class CrFragmentTestUtils {
	/**
	 * Object Mapper
	 */
	public static ObjectMapper mapper = new ObjectMapper();

	/**
	 * Get the reference entry model with the given name
	 * @param name name
	 * @return reference entry model
	 * @throws NodeException
	 */
	public static ContentRepositoryFragmentEntryModel getReferenceEntry(String name) throws NodeException {
		try (InputStream in = CrFragmentTestUtils.class.getResourceAsStream(name)) {
			return mapper.readValue(in, ContentRepositoryFragmentEntryModel.class);
		} catch (IOException e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Assert that the fragment contains the entries
	 * @param fragment fragment
	 * @param entries expected entries
	 * @throws NodeException
	 */
	public static void assertEntries(CrFragment fragment, ContentRepositoryFragmentEntryModel... entries) throws NodeException {
		Trx.consume(c -> assertThat(c.getEntries().stream().map(CrFragmentEntry::getModel).collect(Collectors.toList())).as("Fragment entries")
				.usingElementComparatorIgnoringFields("id", "globalId").contains(entries), fragment);
	}
}
