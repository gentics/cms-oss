package com.gentics.contentnode.object;

import static com.gentics.contentnode.devtools.Synchronizer.mapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.rest.resource.impl.ContentStagingResourceImpl;
import com.gentics.contentnode.staging.StagedReference;

import fi.iki.santtu.md5.MD5;
import fi.iki.santtu.md5.MD5OutputStream;

/**
 * Interface for objects, which are not themselves staged (because they are not content),
 * but are referenced by staged objects (together with a hash for checking integrity while importing)
 */
public interface StagedReferencedObject<T extends NodeObject> {
	/**
	 * Get the global id of the object
	 * @return global id
	 */
	GlobalId getGlobalId();

	/**
	 * Get the staged reference of the object
	 * @return staged reference
	 * @throws NodeException
	 */
	default StagedReference getStagedReference() throws NodeException {
		return new StagedReference().setId(getGlobalId().toString()).setHash(getStagedHash().getLeft());
	}

	/**
	 * Check whether the object matches the staged reference
	 * @param reference reference to check
	 * @return true iff the object currently matches the staged reference
	 * @throws NodeException
	 */
	default boolean matches(StagedReference reference) throws NodeException {
		Pair<String, String> pair = getStagedHash();
		String computedHash = pair.getLeft();
		String representation = pair.getRight();
		boolean matches = StringUtils.equals(reference.getHash(), computedHash);

		if (!matches) {
			// if the hash does not match, try again with the alternate mapper
			Pair<String, String> alternatePair = getStagedHash(true);
			matches = StringUtils.equals(reference.getHash(), alternatePair.getLeft());
		}

		if (!matches) {
			ContentStagingResourceImpl.logger.error(String.format("Implementation hash mismatch for %s: Expected: %s, Actual: %s", this, reference.getHash(), computedHash));
			ContentStagingResourceImpl.logger.error(String.format("Implementation hash for %s is %s, computed from:\n-- begin --\n%s\n-- end --", this, computedHash, representation));
		} else if (ContentStagingResourceImpl.logger.isDebugEnabled()) {
			ContentStagingResourceImpl.logger.debug(String.format("Implementation hash for %s is %s, computed from:\n-- begin --\n%s\n-- end --", this, computedHash, representation));
		}

		return matches;
	}

	/**
	 * Get the hash of the staged reference together with the string representation from which the hash is computed
	 * @return Pair of hash and string representation
	 * @throws NodeException
	 */
	default Pair<String, String> getStagedHash() throws NodeException {
		return getStagedHash(false);
	}

	/**
	 * Get the hash of the staged reference together with the string representation from which the hash is computed
	 * @param alternate true to use the alternate mapper (which will only include non-empty attributes to the JSON)
	 * @return Pair of hash and string representation
	 * @throws NodeException
	 */
	default Pair<String, String> getStagedHash(boolean alternate) throws NodeException {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream();
				MD5OutputStream md5 = new MD5OutputStream(out);
				JsonGenerator jg = new JsonFactory().createGenerator(md5, JsonEncoding.UTF8)) {
			DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter("  ", "\n"));
			jg.setPrettyPrinter(prettyPrinter);

			// get alternate mapper if required
			ObjectMapper mapper = mapper(alternate);
			produceStagedHashComponents(s -> {
				try {
					mapper.writeValue(jg, s);
				} catch (IOException e) {
					throw new NodeException("Error while getting hash for " + this, e);
				}
			});

			return Pair.of(MD5.asHex(md5.hash()).toLowerCase(), out.toString("UTF-8"));
		} catch (NodeException e) {
			throw e;
		} catch (IOException e) {
			throw new NodeException("Error while getting hash for " + this, e);
		}
	}

	/**
	 * Generate all objects, which are part of the staged hash and feed them to the consumer
	 * @param consumer consumer to accept the parts of the staged hash
	 * @throws NodeException
	 */
	void produceStagedHashComponents(Consumer<Object> consumer) throws NodeException;
}
