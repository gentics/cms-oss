package com.gentics.contentnode.rest.model.perm;

/**
 * Enumeration of all available permission types.
 */
public enum PermType {
	/**
	 * Permission to read an object/type
	 */
	read(0),

	/**
	 * Permission to set permissions to subgroups
	 */
	setperm(1),

	/**
	 * Permission to update an object
	 */
	update(2),

	/**
	 * Permission to send instant messages
	 */
	instantmessages(2),

	/**
	 * Permission to create a user
	 */
	createuser(8),

	/**
	 * Permission to update a user
	 */
	updateuser(9),

	/**
	 * Permission to delete (deactivate) a user
	 */
	deleteuser(10),

	/**
	 * Permission to create a group
	 */
	creategroup(8),

	/**
	 * Permission to update a group
	 */
	updategroup(9),

	/**
	 * Permission to delete a group
	 */
	deletegroup(10),

	/**
	 * Permission to change assignment of users to a group (i.e. add users to the group or remove users from the group)
	 */
	userassignment(11),

	/**
	 * Permission to update users in a group
	 */
	updategroupuser(12),

	/**
	 * Permission to change permissions for a group
	 */
	setuserperm(13),

	/**
	 * Permission to assign roles to a group
	 */
	assignroles(10),

	/**
	 * Permissions to display system information
	 */
	sysinfo(8),

	/**
	 * Permission to create on object
	 */
	create(8),

	/**
	 * Permission to delete an object
	 */
	delete(3),

	/**
	 * Permission to delete the error log
	 */
	deleteerrorlog(8),

	/**
	 * Permission to suspend the scheduler
	 */
	suspendscheduler(2),

	/**
	 * Permission to display scheduler task templates
	 */
	readtasktemplates(8),

	/**
	 * Permission to update scheduler task templates
	 */
	updatetasktemplates(9),

	/**
	 * Permission to display scheduler tasks
	 */
	readtasks(10),

	/**
	 * Permission to update scheduler tasks
	 */
	updatetasks(11),

	/**
	 * Permission to display scheduler schedules.
	 */
	readschedules(12),

	/**
	 * Permission to update scheduler schedules
	 */
	updateschedules(13),

	/**
	 * Permission to display scheduler jobs
	 */
	readjobs(12),

	/**
	 * Permission to update scheduler jobs
	 */
	updatejobs(13),

	/**
	 * Permission to update a folder
	 */
	updatefolder(9),

	/**
	 * Permission to delete a folder
	 */
	deletefolder(10),

	/**
	 * Permission to link an overview
	 */
	linkoverview(25),

	/**
	 * Permission to create an overview
	 */
	createoverview(26),

	/**
	 * Permission to display pages/images/files in a folder
	 */
	readitems(11, 10, 10),

	/**
	 * Permission to create pages/images/files in a folder
	 */
	createitems(12, 11, 11),

	/**
	 * Permission to update pages/images/files in a folder
	 */
	updateitems(13, 12, 12),

	/**
	 * Permission to delete pages/images/files in a folder
	 */
	deleteitems(14, 13, 13),

	/**
	 * Permission to import pages/images/files into a folder
	 */
	importitems(23),

	/**
	 * Permission to publish pages in a folder
	 */
	publishpages(19, 14),

	/**
	 * Permission to translate pages in a folder
	 */
	translatepages(-1, 15),

	/**
	 * Permission to display templates in a folder
	 */
	readtemplates(15),

	/**
	 * Permission to create templates in a folder
	 */
	createtemplates(16),

	/**
	 * Permission to update templates in a folder
	 */
	updatetemplates(17),

	/**
	 * Permission to delete templates in a folder
	 */
	deletetemplates(18),

	/**
	 * Permission to link templates to a folder
	 */
	linktemplates(21),

	/**
	 * Permission to update tagtypes
	 */
	updateconstructs(20),

	/**
	 * Permission to perform synchronization between channels
	 */
	channelsync(27),

	/**
	 * Permission to update multichannelling inheritance settings
	 */
	updateinheritance(29),

	/**
	 * Permission to display the wastebin. This includes permission to restore/remove elements from the wastebin.
	 */
	wastebin(28),

	/**
	 * Permission to view forms in a folder
	 */
	viewform(2),

	/**
	 * Permission to create forms in a folder
	 */
	createform(3),

	/**
	 * Permission to update forms in a folder
	 */
	updateform(4),

	/**
	 * Permission to delete forms in a folder
	 */
	deleteform(5),

	/**
	 * Permission to publish forms in a folder
	 */
	publishform(6),

	/**
	 * Permission to view reports for forms in a folder
	 */
	formreport(7),

	/**
	 * Permission to modify the content for a content container
	 */
	modifycontent(11)
	;
	private int bit;

	/**
	 * Page role bit
	 */
	private int pageRoleBit;

	/**
	 * File role bit
	 */
	private int fileRoleBit;

	/**
	 * Create an instance
	 * @param bit permission bit
	 */
	private PermType(int bit) {
		this(bit, -1, -1);
	}

	/**
	 * Create an instance with bit and page role bit
	 * @param bit permission bit
	 * @param pageRoleBit page role bit
	 */
	private PermType(int bit, int pageRoleBit) {
		this(bit, pageRoleBit, -1);
	}

	/**
	 * Create an instance with permbit and rolebits
	 * @param bit permission bit
	 * @param pageRoleBit page role bit
	 * @param fileRoleBit file role bit
	 */
	private PermType(int bit, int pageRoleBit, int fileRoleBit) {
		this.bit = bit;
		this.pageRoleBit = pageRoleBit;
		this.fileRoleBit = fileRoleBit;
	}

	/**
	 * Get the permission bit
	 * @return permission bit (may be -1 to indicate permissions, that can only be set to roles)
	 */
	public int getBit() {
		return bit;
	}

	/**
	 * Get the role permission bit to set on pages
	 * @return role permission bit (may be -1 to indicate permissions, that cannot be set to roles for pages)
	 */
	public int getPageRoleBit() {
		return pageRoleBit;
	}

	/**
	 * Get the role permission bit to set on files
	 * @return role permission bit (may be -1 to indicate permissions, that cannot be set to roles for files)
	 */
	public int getFileRoleBit() {
		return fileRoleBit;
	}
}
