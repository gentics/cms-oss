package com.gentics.contentnode.mcp.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.resource.impl.ConstructResourceImpl;

/**
 * Looks up constructs by keyword, which the CMS has no REST route for, and the nodes of a construct, which
 * {@link ConstructResourceImpl#listConstructNodes} only lists with the unrelated object property admin permission
 */
public final class Constructs {
	private Constructs() {
	}

	/**
	 * Get the ID of the construct with the given keyword, without checking any permission. Must be called in a
	 * transaction.
	 * @param keyword construct keyword
	 * @return ID, null if no construct has the keyword
	 * @throws NodeException
	 */
	public static Integer idByKeyword(String keyword) throws NodeException {
		return DBUtils.select("SELECT id FROM construct WHERE keyword = ?", pst -> pst.setString(1, keyword),
				rs -> rs.next() ? rs.getInt("id") : null);
	}

	/**
	 * Get the ID of the construct with the given keyword, in an own transaction, without checking any permission. The
	 * delegate called with the ID checks them.
	 * @param keyword construct keyword
	 * @return ID
	 * @throws EntityNotFoundException if no construct has the keyword
	 * @throws NodeException
	 */
	public static int resolve(String keyword) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Integer id = idByKeyword(keyword);
			trx.success();
			if (id == null) {
				throw notFound(keyword);
			}
			return id;
		}
	}

	/**
	 * Get the construct with the given keyword. Must be called in a transaction.
	 * @param keyword construct keyword
	 * @return construct
	 * @throws EntityNotFoundException if no construct has the keyword
	 * @throws NodeException
	 */
	public static Construct byKeyword(String keyword) throws NodeException {
		Integer id = idByKeyword(keyword);
		Construct construct = id != null ? TransactionManager.getCurrentTransaction().getObject(Construct.class, id)
				: null;
		if (construct == null) {
			throw notFound(keyword);
		}
		return construct;
	}

	/**
	 * Get the nodes a construct is assigned to and the caller can view, in an own transaction
	 * @param constructId construct ID
	 * @return node refs
	 * @throws EntityNotFoundException if the construct does not exist
	 * @throws NodeException
	 */
	public static List<ObjectRef> nodeRefs(int constructId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Construct construct = trx.getTransaction().getObject(Construct.class, constructId);
			if (construct == null) {
				throw new EntityNotFoundException("Construct %d does not exist".formatted(constructId));
			}
			List<ObjectRef> refs = new ArrayList<>();
			for (Node node : construct.getNodes()) {
				if (ObjectPermission.view.checkObject(node)) {
					refs.add(ObjectRef.forNode(Node.TRANSFORM2REST.apply(node)));
				}
			}
			trx.success();
			return refs;
		}
	}

	/**
	 * Check that the caller may view the nodes and change their constructs ({@code updateconstructs}), in an own
	 * transaction. The construct delegates do not check this for nodes a construct is added to.
	 * @param nodeIds node IDs
	 * @throws EntityNotFoundException if a node does not exist
	 * @throws InsufficientPrivilegesException if the caller lacks the permission on a node
	 * @throws NodeException
	 */
	public static void checkUpdateConstructs(Collection<Integer> nodeIds) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			for (int nodeId : nodeIds) {
				Node node = trx.getTransaction().getObject(Node.class, nodeId);
				if (node == null) {
					throw new EntityNotFoundException("Node %d does not exist".formatted(nodeId));
				}
				if (!ObjectPermission.view.checkObject(node) || !ObjectPermission.updateconstructs.checkObject(node)) {
					throw new InsufficientPrivilegesException(
							"Missing permission to change the constructs of node %d (updateconstructs)"
									.formatted(nodeId),
							node, PermType.updateconstructs);
				}
			}
			trx.success();
		}
	}

	/**
	 * Build the exception for an unknown keyword
	 * @param keyword keyword
	 * @return exception
	 */
	private static EntityNotFoundException notFound(String keyword) {
		return new EntityNotFoundException("No construct has the keyword '%s', see list_constructs".formatted(keyword));
	}
}
