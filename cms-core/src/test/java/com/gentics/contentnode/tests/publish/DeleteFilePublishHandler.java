package com.gentics.contentnode.tests.publish;

import java.util.Map;

import com.gentics.api.contentnode.publish.CnMapPublishException;
import com.gentics.api.contentnode.publish.CnMapPublishHandler;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.factory.InstantPublishingTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.TransactionManager.Executable;
import com.gentics.contentnode.object.File;

public class DeleteFilePublishHandler implements CnMapPublishHandler {
	protected int id;

	@Override
	public void init(Map parameters) throws CnMapPublishException {
		id = ObjectTransformer.getInt(parameters.get("id"), 0);
	}

	@Override
	public void open(long timestamp) throws CnMapPublishException {
	}

	@Override
	public void createObject(Resolvable object) throws CnMapPublishException {
		try {
			TransactionManager.execute(new Executable() {
				@Override
				public void execute() throws NodeException {
					try (InstantPublishingTrx iTrx = new InstantPublishingTrx(false)) {
						Transaction t = TransactionManager.getCurrentTransaction();
						File file = t.getObject(File.class, id);
						if (file != null) {
							file.delete();
						}
						t.commit(false);
					}
				}
			});
		} catch (NodeException e) {
			throw new CnMapPublishException("Error while deleting object when it should be published", e);
		}
	}

	@Override
	public void updateObject(Resolvable object) throws CnMapPublishException {
	}

	@Override
	public void deleteObject(Resolvable object) throws CnMapPublishException {
	}

	@Override
	public void commit() {
	}

	@Override
	public void rollback() {
	}

	@Override
	public void close() {
	}

	@Override
	public void destroy() {
	}
}
