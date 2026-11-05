package com.gentics.contentnode.factory.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.object.Node;

import java.util.Map;

/**
 * Interface for extending wastebin functionality to other node items.
 */
public interface WastebinService {

    /**
     * Purges items in the given node.
     * @param t The current transaction to delete the items in.
     * @param node The node from where the items should be purged from.
     * @param deletedBefore get all objects, that were deleted before this timestamp
     * @return A Map with the item-type id as key, and the amount of items deleted as value.
     * @throws NodeException
     */
    Map<String, Integer> purgeInNode(Transaction t, Node node, int deletedBefore) throws NodeException;
}
