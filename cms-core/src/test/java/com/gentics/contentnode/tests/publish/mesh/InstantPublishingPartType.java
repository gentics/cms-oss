package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;

import com.gentics.api.contentnode.parttype.AbstractExtensiblePartType;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.resolving.StackResolvable;

/**
 * PartType implementation that will modify (and republish) the object.
 * This will have the object instant published (while the object is currently rendered during the publish process) 
 */
public class InstantPublishingPartType extends AbstractExtensiblePartType {
	public static boolean doInstantPublish = false;

	@Override
	public String render() throws NodeException {
		if (doInstantPublish) {
			Transaction t = TransactionManager.getCurrentTransaction();
			StackResolvable object = t.getRenderType().getRenderedRootObject();
			if (object instanceof Folder) {
				doInstantPublish = false;
				Trx.operate(() -> {
					update((Folder)object, upd -> {
						upd.setName("Modified " + upd.getName());
					});
				});
			} else if (object instanceof File) {
				doInstantPublish = false;
				Trx.operate(() -> {
					update((File)object, upd -> {
						upd.setName("Modified " + upd.getName());
					});
				});
			} else if (object instanceof Page) {
				doInstantPublish = false;
				Trx.operate(() -> {
					update((Page)object, upd -> {
						upd.setName("Modified " + upd.getName());
					});
				});
			}
		}

		return "";
	}
}
