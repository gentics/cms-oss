package com.gentics.contentnode.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.RandomUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.db.DBUtils.HandleSelectResultSet;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.rest.model.token.APITokenCreationRequest;
import com.gentics.contentnode.rest.model.token.APITokenDataModel;

/**
 * Factory for management of API Tokens
 */
public class APITokenFactory {
	/**
	 * Byte count for generated tokens
	 */
	public final static int BYTE_COUNT = 32;

	/**
	 * Name of the table for storing token data
	 */
	protected final static String TABLE_NAME = "api_token";

	/**
	 * Select clause
	 */
	protected final static String SELECT_CLAUSE = "SELECT id, name, cdate, expires, last_used FROM %s".formatted(TABLE_NAME);

	/**
	 * SQL to insert a new record
	 */
	protected final static String INSERT_SQL = "INSERT INTO %s (user_id, name, token_hash, cdate, expires) VALUES (?, ?, ?, ?, ?)".formatted(TABLE_NAME);

	/**
	 * SQL to delete a record
	 */
	protected final static String DELETE_SQL = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);

	/**
	 * Instance of {@link HandleSelectResultSet} which creates an instance of {@link APITokenDataModel} from the current row of the {@link ResultSet}
	 */
	protected final static DBUtils.HandleSelectResultSet<ResolvableAPITokenDataModel> ROW_HANDLER = rs -> {
		ResolvableAPITokenDataModel model = new ResolvableAPITokenDataModel();
		int now = TransactionManager.getCurrentTransaction().getUnixTimestamp();
		int expiry = rs.getInt("expires");
		model
			.setId(rs.getInt("id"))
			.setName(rs.getString("name"))
			.setCdate(rs.getInt("cdate"))
			.setExpires(expiry)
			.setLastUsed(rs.getInt("last_used"))
			.setValid(expiry > 0 ? expiry > now : true);

		return model;
	};

	/**
	 * Create a new token
	 * @return new token
	 */
	public final static String createToken() {
		byte[] randomBytes = RandomUtils.secureStrong().randomBytes(BYTE_COUNT);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}

	/**
	 * Get the hash of the given token
	 * @param token token
	 * @return hash
	 * @throws NodeException
	 */
	public final static String hash(String token) throws NodeException {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new NodeException("Error while hashing API Token", e);
		}
	}

	/**
	 * Create a new API Token for the given user in the database
	 * @param request create request
	 * @param userId user ID
	 * @param token token
	 * @return stored instance
	 * @throws NodeException
	 */
	public final static ResolvableAPITokenDataModel create(APITokenCreationRequest request, int userId, String token)
			throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		int cDate = t.getUnixTimestamp();
		String hash = hash(token);

		List<Integer> ids = DBUtils.executeInsert(
				INSERT_SQL,
				new Object[] { userId, request.getName(), hash, cDate, request.getExpires() });

		if (ids.size() != 1) {
			throw new NodeException("Error while creating API Token. Unexpected number of inserts: %d".formatted(ids.size()));
		}

		int id = ids.get(0);

		Optional<ResolvableAPITokenDataModel> optData = DBUtils.select(SELECT_CLAUSE + " WHERE id = ?", pst -> {
			pst.setInt(1, id);
		}, DBUtils.getFirst(ROW_HANDLER));

		if (optData.isEmpty()) {
			throw new NodeException("Error while creating API Token.");
		}

		return optData.get();
	}

	/**
	 * Load the token with given ID for the user
	 * @param userId user ID
	 * @param tokenId token ID
	 * @return optional token instance
	 * @throws NodeException
	 */
	public final static Optional<ResolvableAPITokenDataModel> load(int userId, int tokenId) throws NodeException {
		return DBUtils.select(SELECT_CLAUSE + " WHERE id = ? AND user_id = ?", pst -> {
			pst.setInt(1, tokenId);
			pst.setInt(2, userId);
		}, DBUtils.getFirst(ROW_HANDLER));
	}

	/**
	 * Get the list of API Tokens for the given user
	 * @param userId user ID
	 * @return list of tokens
	 * @throws NodeException
	 */
	public final static List<ResolvableAPITokenDataModel> list(int userId) throws NodeException {
		return DBUtils.select(SELECT_CLAUSE + " WHERE user_id = ?", pst -> {
			pst.setInt(1, userId);
		}, DBUtils.getAll(ROW_HANDLER));
	}

	/**
	 * Delete the token with given ID
	 * @param tokenId token ID
	 * @throws NodeException
	 */
	public final static void delete(int tokenId) throws NodeException {
		DBUtils.update(DELETE_SQL, tokenId);
	}
}
