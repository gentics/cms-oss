/* Generated By:JJTree: Do not edit this line. SimpleNode.java */

package com.gentics.lib.expressionparser.parser;

public class SimpleNode implements Node {
	protected Node parent;
	protected Node[] children;
	protected int id;
	protected Parser parser;

	protected int startLine;
	protected int startColumn;

	public SimpleNode(int i) {
		id = i;
	}

	public SimpleNode(Parser p, int i) {
		this(i);
		// if(p.jj_input_stream.bufpos >= 0) {
		// startLine = p.jj_input_stream.bufline[p.jj_input_stream.bufpos];
		// startColumn = p.jj_input_stream.bufcolumn[p.jj_input_stream.bufpos];
		// System.out.println("node " + getClass().getName() + ", line " + startLine + ", col " + startColumn);
		// System.out.println(p.jj_input_stream.GetImage());
		// }
	}

	public void jjtOpen() {}

	public void jjtClose() {}
  
	public void jjtSetParent(Node n) {
		parent = n;
	}

	public Node jjtGetParent() {
		return parent;
	}

	public void jjtAddChild(Node n, int i) {
		if (children == null) {
			children = new Node[i + 1];
		} else if (i >= children.length) {
			Node c[] = new Node[i + 1];

			System.arraycopy(children, 0, c, 0, children.length);
			children = c;
		}
		children[i] = n;
	}

	public Node jjtGetChild(int i) {
		return children[i];
	}

	public int jjtGetNumChildren() {
		return (children == null) ? 0 : children.length;
	}

	/* You can override these two methods in subclasses of SimpleNode to
	 customize the way the node appears when the tree is dumped.  If
	 your output uses more than one line you should override
	 toString(String), otherwise overriding toString() is probably all
	 you need to do. */

	public String toString() {
		return ParserTreeConstants.jjtNodeName[id];
	}

	public String toString(String prefix) {
		return prefix + toString();
	}

	/* Override this method if you want to customize how the node dumps
	 out its children. */

	public void dump(String prefix) {
		System.out.println(toString(prefix));
		if (children != null) {
			for (int i = 0; i < children.length; ++i) {
				SimpleNode n = (SimpleNode) children[i];

				if (n != null) {
					n.dump(prefix + " ");
				}
			}
		}
	}
}

