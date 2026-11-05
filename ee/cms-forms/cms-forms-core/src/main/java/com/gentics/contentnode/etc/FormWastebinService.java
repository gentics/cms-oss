package com.gentics.contentnode.etc;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.WastebinService;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.model.NodeFeature;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.log.NodeLogger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FormWastebinService implements WastebinService {
    private final static NodeLogger wastebinLogger = NodeLogger.getNodeLogger("wastebin");

    @Override
    public Map<String, Integer> purgeInNode(Transaction t, Node node, int deletedBefore) throws NodeException {
        if (!node.getFeatures().contains(Feature.FORMS)) {
            return Map.of();
        }

        List<Form> forms = getFormsToPurge(node, deletedBefore);
        int formCount = forms.size();
        for (Form form : forms) {
            form.delete(true);
            t.commit(false);
        }
        wastebinLogger.info("Purged " + formCount + (formCount == 1 ? " form": " forms"));

        return Map.of("form", formCount);
    }

    private static List<Form> getFormsToPurge(Node node, int deletedBefore) throws NodeException {
        int nodeId = node.getMaster().getId();
        List<Integer> ids = new ArrayList<Integer>();
        DBUtils.executeStatement(
                "SELECT form.id FROM form form, folder f WHERE form.folder_id = f.id AND f.node_id = ? AND form.deleted > 0 AND form.deleted < ?",
                new SQLExecutor() {
                    @Override
                    public void prepareStatement(PreparedStatement stmt) throws SQLException {
                        stmt.setInt(1, nodeId);
                        stmt.setInt(2, deletedBefore);
                    }

                    @Override
                    public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
                        while (rs.next()) {
                            ids.add(rs.getInt("id"));
                        }
                    }
                });

        return TransactionManager.getCurrentTransaction().getObjects(Form.class, ids);
    }
}