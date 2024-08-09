package com.gentics.contentnode.log;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.rest.model.response.log.ActionModel;

/**
 * Logged actions
 */
public enum Action {
	delete(ActionLogger.DEL, "340.delete"),

	edit(ActionLogger.EDIT, "339.edit"),

	create(ActionLogger.CREATE, "338.create"),

	move(ActionLogger.MOVE, "387.move"),

	perm(ActionLogger.PERM, "permissions"),

	login(ActionLogger.LOGIN, "login"),

	logout(ActionLogger.LOGOUT, "logout"),

	publish(ActionLogger.PAGEPUB, "cn_page_publish"),

	view(ActionLogger.VIEW, "view"),

	pageimport(ActionLogger.IMPORT, "cn_html_import"),

	dirt(ActionLogger.DIRT, "followed publish dependencies"),

	restore(ActionLogger.RESTORE, "restore"),

	timemanagement(ActionLogger.PAGETIME, "cn_timemanagement"),

	queue(ActionLogger.PAGEQUEUE, "cn_site_in_queue"),

	copy(ActionLogger.COPY, "copy"),

	// TODO remove?
	notify(ActionLogger.NOTIFY, "page update"),

	deleteversions(ActionLogger.DELALLVERSIONS, "logcmd.delete.allversions"),

	generate(ActionLogger.GENERATE, "w_generate"),

	offline(ActionLogger.PAGEOFFLINE, "cn_pages_offline"),

	version(ActionLogger.VERSION, "logcmd.create.version"),

	majorversion(ActionLogger.MAJORVERSION, "logcmd.create.majorversion"),

	lock(ActionLogger.LOCK, "logcmd.lock"),

	unlock(ActionLogger.UNLOCK, "logcmd.unlock"),

	wastebin(ActionLogger.WASTEBIN, "logcmd.wastebin"),

	wastebinrestore(ActionLogger.WASTEBINRESTORE, "logcmd.wastebin.restore"),

	hide(ActionLogger.MC_HIDE, "inheritance.remove"),

	unhide(ActionLogger.MC_UNHIDE, "inheritance.add"),

	loginfailed(ActionLogger.LOGIN_FAILED, "login.failed"),

	accessdenied(ActionLogger.ACCESS_DENIED, "access.denied"),

	publish_start(ActionLogger.PUBLISH_START, "logcmd.publish_start"),

	publish_node_start(ActionLogger.PUBLISH_NODE_START, "logcmd.publish_node_start"),

	publish_done(ActionLogger.PUBLISH_RUN, "logcmd.publish_done"),

	maintenance(ActionLogger.MAINTENANCE, "logcmd.maintenance"),

	fum_start(ActionLogger.FUM_START, "logcmd.fum.start"),

	fum_accepted(ActionLogger.FUM_ACCEPTED, "logcmd.fum.accepted"),

	fum_denied(ActionLogger.FUM_DENIED, "logcmd.fum.denied"),

	fum_postponed(ActionLogger.FUM_POSTPONED, "logcmd.fum.postponed"),

	fum_error(ActionLogger.FUM_ERROR, "logcmd.fum.error"),

	purgelogs(ActionLogger.PURGELOGS, "logcmd.purgelogs"),

	purgemessages(ActionLogger.PURGEMESSAGES, "logcmd.purgemessages"),

	inboxcreate(ActionLogger.INBOXCREATE, "logcmd.inbox"),

	devtool_sync_start(ActionLogger.DEVTOOL_SYNC_START, "devtools.sync.start"),

	devtool_sync_end(ActionLogger.DEVTOOL_SYNC_END, "devtools.sync.end"),

	;

	/**
	 * Transform the action into its REST model
	 */
	public final static BiFunction<Action, ActionModel, ActionModel> NODE2REST = (action, model) -> {
		model.setName(action.name());
		model.setLabel(action.getLabel());
		return model;
	};

	/**
	 * Transform the action into its REST model
	 */
	public final static Function<Action, ActionModel> TRANSFORM2REST = action -> {
		return NODE2REST.apply(action, new ActionModel());
	};

	/**
	 * Get the action by code, or null if not found
	 * @param code code
	 * @return action or null
	 */
	public final static Action getByCode(int code) {
		for (Action action : values()) {
			if (action.code == code) {
				return action;
			}
		}
		return null;
	}

	/**
	 * Action code (aka cmd_desc_id)
	 */
	private int code;

	/**
	 * I18n key for translation
	 */
	private String i18nKey;

	/**
	 * Create an instance
	 * @param code code
	 * @param i18nKey translation key
	 */
	private Action(int code, String i18nKey) {
		this.code = code;
		this.i18nKey = i18nKey;
	}

	/**
	 * Get action code
	 * @return code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Translated label
	 * @return label
	 */
	public String getLabel() {
		return I18NHelper.get(i18nKey);
	}
}
