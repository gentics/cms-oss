package com.gentics.contentnode.rest.model.request;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * Permissions
 */
@XmlEnum(String.class)
public enum Permission {
	read,
	view,
	setperm,
	create,
	delete,
	update,
	edit,
	publish,
	createfolder,
	updatefolder,
	deletefolder,
	linkoverview,
	createoverview,
	readitems,
	createitems,
	updateitems,
	deleteitems,
	importitems,
	publishpages,
	translatepages,
	viewform,
	createform,
	updateform,
	deleteform,
	publishform,
	formreport,
	readtemplates,
	createtemplates,
	updatetemplates,
	deletetemplates,
	linktemplates,
	updateconstructs,
	channelsync,
	updateinheritance,
	wastebin,
	inheritance,
	userassignment
}
