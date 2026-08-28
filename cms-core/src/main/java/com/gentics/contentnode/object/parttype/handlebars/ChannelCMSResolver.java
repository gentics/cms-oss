package com.gentics.contentnode.object.parttype.handlebars;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.CMSResolver;

public class ChannelCMSResolver extends CMSResolver {

	static {
		properties.put("channel", new Property() {
			public Object get(CMSResolver cmsResolver) {
				if (cmsResolver instanceof ChannelCMSResolver channeled) {
					return channeled.getChannel();
				}
				return null;
			}
		});
	}

	protected Node channel;

	public ChannelCMSResolver(CMSResolver cmsResolver, Node channel) throws NodeException {
		this(
			(Page) cmsResolver.get("page"),
			(Template) cmsResolver.get("template"),
			(Tag) cmsResolver.get("tag"),
			(Folder) cmsResolver.get("folder"),
			(Node) cmsResolver.get("node"),
			(ContentFile) cmsResolver.get("file"),
			channel
		);
	}

	public ChannelCMSResolver(Page page, Template template, Tag tag, Folder folder, Node node, ContentFile file, Node channel)
			throws NodeException {
		super(page, template, tag, folder, node, file);

		this.channel = channel;
	}

	protected Node getChannel() {
		return channel;
	}
}
