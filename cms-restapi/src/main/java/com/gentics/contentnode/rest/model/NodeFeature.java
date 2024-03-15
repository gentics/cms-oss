package com.gentics.contentnode.rest.model;

/**
 * Features that can be activated per node
 */
public enum NodeFeature {

	/**
	 * If this feature is activated, images and files that are not used by other objects (pages or folders), will not be published
	 */
	contentfile_auto_offline,
	/**
	 * If this feature is activated, the backend will not ask if it should localize or edit the inherited object, but it will just localize it.'
	 */
	always_localize,
	/**
	 * If this feature is activated, during instant publishing, pages will not be removed from the content repository
	 */
	disable_instant_delete,
	/**
	 * If this feature is activated, the startpage of parentfolders of instant published pages will also be published during instant publish runs
	 */
	publish_folder_startpage,

	/**
	 * If this feature is activated live urls will be shown for objects in the node
	 */
	live_urls_per_node,

	/**
	 * When this feature is activated, external URLs in pages of the node will be checked for validity
	 */
	link_checker,

	/**
	 * When this feature is activated, the node may contain forms
	 */
	forms,

	/**
	 * With this feature, the node has additional asset management enabled (which must be configured)
	 */
	asset_management,

	/**
	 * If this feature is activated, uploaded images are automatically converted to WebP.
	 */
	webp_conversion,
	/*
	 * With this feature, the properties editor will immediately open upon uploading a new file.
	 */
	upload_file_properties,

	/**
	 * With this feature, the properties editor will immediately open upon uploading a new image.
	 */
	upload_image_properties
}
