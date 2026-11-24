package com.gentics.contentnode.testutils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.object.FolderService;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.OpResult;

/**
 * Implementation of {@link FolderService} for tests, which allows to verify the calls to
 * {@link FolderService#checkMoveToOtherNode(Folder, Folder, Collection)} and to veto moving folders.
 * By default (no handler set), the implementation does nothing and allows all moves.
 */
public class TestFolderMoveCheckService implements FolderService {
	/**
	 * Handler, which is called for every check (may be null)
	 */
	protected static volatile MoveCheckHandler handler;

	/**
	 * Recorded calls (only recorded while a handler is set)
	 */
	protected static final List<MoveCheckCall> calls = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Set the handler and clear the recorded calls
	 * @param handler handler (may be null to reset)
	 */
	public static void setHandler(MoveCheckHandler handler) {
		calls.clear();
		TestFolderMoveCheckService.handler = handler;
	}

	/**
	 * Reset the handler and clear the recorded calls
	 */
	public static void reset() {
		setHandler(null);
	}

	/**
	 * Get a copy of the recorded calls
	 * @return list of recorded calls
	 */
	public static List<MoveCheckCall> getCalls() {
		synchronized (calls) {
			return new ArrayList<>(calls);
		}
	}

	@Override
	public OpResult checkMoveToOtherNode(Folder folder, Folder target, Collection<Integer> subFolderIds) throws NodeException {
		MoveCheckHandler currentHandler = handler;
		if (currentHandler == null) {
			return OpResult.OK;
		}
		calls.add(new MoveCheckCall(folder.getId(), target.getId(), new ArrayList<>(subFolderIds)));
		return currentHandler.check(folder, target, subFolderIds);
	}

	/**
	 * Handler interface
	 */
	@FunctionalInterface
	public static interface MoveCheckHandler {
		/**
		 * Check the move
		 * @param folder moved folder
		 * @param target target folder
		 * @param subFolderIds IDs of the moved folder structure
		 * @return result (may be null)
		 * @throws NodeException
		 */
		OpResult check(Folder folder, Folder target, Collection<Integer> subFolderIds) throws NodeException;
	}

	/**
	 * Recorded call
	 */
	public static class MoveCheckCall {
		public final int folderId;

		public final int targetId;

		public final List<Integer> subFolderIds;

		public MoveCheckCall(int folderId, int targetId, List<Integer> subFolderIds) {
			this.folderId = folderId;
			this.targetId = targetId;
			this.subFolderIds = subFolderIds;
		}
	}
}
