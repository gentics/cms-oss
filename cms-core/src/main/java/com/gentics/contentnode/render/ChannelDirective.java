package com.gentics.contentnode.render;

import java.io.IOException;
import java.io.Writer;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;

/**
 * Velocity directive that renders its body in the scope of the given channel.
 */
public class ChannelDirective extends Directive {

	/*
	 * (non-Javadoc)
	 * @see org.apache.velocity.runtime.directive.Directive#getName()
	 */
	@Override
	public String getName() {
		return "gtx_channel";
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.velocity.runtime.directive.Directive#getType()
	 */
	@Override
	public int getType() {
		return BLOCK;
	}

	/**
	 * This directive will render its body in the scope of the given channel.
	 *
	 * @see org.apache.velocity.runtime.directive.Directive#render(org.apache.velocity.context.InternalContextAdapter,
	 * java.io.Writer, org.apache.velocity.runtime.parser.node.Node)
	 * @see ChannelTrx
	 */
	@Override
	public boolean render(final InternalContextAdapter context, final Writer writer, final Node node)
			throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {
		final Node param = node.jjtGetChild(0);
		final Node body = node.jjtGetChild(node.jjtGetNumChildren() - 1);

		final Object nodeIdRaw = param.value(context);
		final String nodeIdString = ObjectTransformer.getString(nodeIdRaw, null);

		if (ObjectTransformer.isEmpty(nodeIdString)) {
			return body.render(context, writer);
		}

		com.gentics.contentnode.object.Node channel = getNodeFromIdOrThrowException(nodeIdString,
				context, param);

		try (final ChannelTrx trx = new ChannelTrx(channel)) {
			return body.render(context, writer);
		} catch (final NodeException e) {
			throw new IOException(e);
		}
	}

	private com.gentics.contentnode.object.Node getNodeFromIdOrThrowException(String nodeIdString,
																			  InternalContextAdapter context,
																			  Node param) {
		try {
			final Transaction currentTransaction = TransactionManager.getCurrentTransaction();

			com.gentics.contentnode.object.Node node = currentTransaction.getObject(
				com.gentics.contentnode.object.Node.class,
				nodeIdString,
				false,
				false);

			if (node == null) {
				throw new ResourceNotFoundException("Node with ID {" + nodeIdString + "} not found");
			}

			return node;
		} catch (final NodeException e) {
			throw new MethodInvocationException(
					"Could not load Node with ID {" + nodeIdString + "}",
					e,
					getName(),
					context.getCurrentTemplateName(),
					param.getLine(),
					param.getColumn());
		}
	}

}
