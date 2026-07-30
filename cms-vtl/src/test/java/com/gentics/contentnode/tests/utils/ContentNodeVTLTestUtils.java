package com.gentics.contentnode.tests.utils;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;

import java.io.IOException;
import java.util.List;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.BreadcrumbPartType;
import com.gentics.contentnode.object.parttype.FolderURLPartType;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.NavigationPartType;
import com.gentics.contentnode.object.parttype.VelocityPartType;
import com.gentics.lib.etc.StringUtils;

public class ContentNodeVTLTestUtils {
	/**
	 * Name of the template part for a velocity construct
	 */
	public static final String TEMPLATE_PARTNAME = "template";

	/**
	 * Name of the startfolder part for a breadcrumb/navigation construct
	 */
	public static final String STARTFOLDER_PARTNAME = "startfolder";

	/**
	 * Creates a velocity construct
	 *
	 * @return
	 * @throws NodeException
	 */
	public static Construct createVelocityConstruct(Node node) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Construct construct = t.createObject(Construct.class);
		construct.setKeyword("constr");
		construct.setName("Construct (de)", 1);
		construct.setName("Construct (en)", 2);
		construct.getNodes().add(node);

		Part velPart = t.createObject(Part.class);
		velPart.setKeyname("velocity");
		velPart.setHidden(false);
		velPart.setEditable(0);
		velPart.setName("velocity", 1);
		velPart.setName("velocity", 2);
		velPart.setPartOrder(1);
		velPart.setPartTypeId(33);

		Part tplPart = t.createObject(Part.class);
		tplPart.setKeyname("template");
		tplPart.setHidden(true);
		tplPart.setEditable(0);
		tplPart.setName("tpl", 1);
		tplPart.setName("tpl", 2);
		tplPart.setPartOrder(2);
		tplPart.setPartTypeId(21);

		Part textPart = t.createObject(Part.class);
		textPart.setKeyname("text");
		textPart.setHidden(true);
		textPart.setEditable(2);
		textPart.setName("Text", 1);
		textPart.setName("Text", 2);
		textPart.setPartOrder(3);
		textPart.setPartTypeId(1);

		List<Part> parts = construct.getParts();
		parts.add(velPart);
		parts.add(tplPart);
		parts.add(textPart);

		Value vval = t.createObject(Value.class);
		vval.setContainer(construct);
		vval.setPart(tplPart);
		vval.setValueText("");
		tplPart.setDefaultValue(vval);

		construct.save();

		return construct;
	}

	/**
	 * Create a construct containing a velocity part
	 * @param node node
	 * @param constructKeyword construct keyword
	 * @param partKeyword part keyword for the velocity part
	 * @return construct id
	 * @throws NodeException
	 */
	public static int createVelocityConstruct(Node node, String constructKeyword, String partKeyword) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Construct construct = t.createObject(Construct.class);
		construct.setAutoEnable(true);
		construct.setKeyword(constructKeyword);
		construct.setName(constructKeyword, 1);
		if (node != null) {
			construct.getNodes().add(node);
		}

		Part vtlPart = t.createObject(Part.class);
		vtlPart.setEditable(0);
		vtlPart.setHidden(false);
		vtlPart.setKeyname(partKeyword);
		vtlPart.setName(partKeyword, 1);
		vtlPart.setPartTypeId(getPartTypeId(VelocityPartType.class));
		construct.getParts().add(vtlPart);

		Part templatePart = t.createObject(Part.class);
		templatePart.setEditable(1);
		templatePart.setHidden(true);
		templatePart.setKeyname(TEMPLATE_PARTNAME);
		templatePart.setName(TEMPLATE_PARTNAME, 1);
		templatePart.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
		t.createObject(Value.class).setPart(templatePart);
		construct.getParts().add(templatePart);

		construct.save();
		t.commit(false);

		return ObjectTransformer.getInt(construct.getId(), 0);
	}

	/**
	 * Create a default breadcrumb construct
	 * @param node node
	 * @param constructKeyword construct keyword
	 * @param partKeyword part keyword
	 * @return construct id
	 * @throws NodeException
	 */
	public static int createBreadcrumbConstruct(Node node, String constructKeyword, String partKeyword) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Construct construct = t.createObject(Construct.class);
		construct.setAutoEnable(true);
		construct.setKeyword(constructKeyword);
		construct.setName(constructKeyword, 1);
		construct.getNodes().add(node);

		// breadcrumb part
		Part vtlPart = t.createObject(Part.class);
		vtlPart.setEditable(0);
		vtlPart.setHidden(false);
		vtlPart.setKeyname(partKeyword);
		vtlPart.setName(partKeyword, 1);
		vtlPart.setPartTypeId(getPartTypeId(BreadcrumbPartType.class));
		construct.getParts().add(vtlPart);

		// template part
		Part templatePart = t.createObject(Part.class);
		templatePart.setEditable(1);
		templatePart.setHidden(true);
		templatePart.setKeyname(TEMPLATE_PARTNAME);
		templatePart.setName(TEMPLATE_PARTNAME, 1);
		templatePart.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
		templatePart.setDefaultValue(t.createObject(Value.class));
		try {
			templatePart.getDefaultValue().setValueText(StringUtils.readStream(ContentNodeTestDataUtils.class.getResourceAsStream("breadcrumb.vm")));
		} catch (IOException e) {
			throw new NodeException("Could not create breadcrumb part", e);
		}
		construct.getParts().add(templatePart);

		// startfolder part
		Part startfolderPart = t.createObject(Part.class);
		startfolderPart.setEditable(1);
		startfolderPart.setHidden(true);
		startfolderPart.setKeyname(STARTFOLDER_PARTNAME);
		startfolderPart.setName(STARTFOLDER_PARTNAME, 1);
		startfolderPart.setPartTypeId(getPartTypeId(FolderURLPartType.class));
		startfolderPart.setDefaultValue(t.createObject(Value.class));
		construct.getParts().add(startfolderPart);

		construct.save();
		t.commit(false);

		return ObjectTransformer.getInt(construct.getId(), 0);
	}

	/**
	 * Create a default navigation construct
	 * 
	 * @param node             node
	 * @param constructKeyword construct keyword
	 * @param partKeyword      part keyword
	 * @return construct id
	 * @throws NodeException
	 */
	public static int createNavigationConstruct(Node node, String constructKeyword, String partKeyword)
			throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Construct construct = t.createObject(Construct.class);
		construct.setAutoEnable(true);
		construct.setKeyword(constructKeyword);
		construct.setName(constructKeyword, 1);
		construct.getNodes().add(node);

		// breadcrumb part
		Part vtlPart = t.createObject(Part.class);
		vtlPart.setEditable(0);
		vtlPart.setHidden(false);
		vtlPart.setKeyname(partKeyword);
		vtlPart.setName(partKeyword, 1);
		vtlPart.setPartTypeId(getPartTypeId(NavigationPartType.class));
		construct.getParts().add(vtlPart);

		// template part
		Part templatePart = t.createObject(Part.class);
		templatePart.setEditable(1);
		templatePart.setHidden(true);
		templatePart.setKeyname(TEMPLATE_PARTNAME);
		templatePart.setName(TEMPLATE_PARTNAME, 1);
		templatePart.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
		templatePart.setDefaultValue(t.createObject(Value.class));
		try {
			templatePart.getDefaultValue().setValueText(
					StringUtils.readStream(ContentNodeTestDataUtils.class.getResourceAsStream("navigation.vm")));
		} catch (IOException e) {
			throw new NodeException("Could not create breadcrumb part", e);
		}
		construct.getParts().add(templatePart);

		// startfolder part
		Part startfolderPart = t.createObject(Part.class);
		startfolderPart.setEditable(1);
		startfolderPart.setHidden(true);
		startfolderPart.setKeyname(STARTFOLDER_PARTNAME);
		startfolderPart.setName(STARTFOLDER_PARTNAME, 1);
		startfolderPart.setPartTypeId(getPartTypeId(FolderURLPartType.class));
		startfolderPart.setDefaultValue(t.createObject(Value.class));
		construct.getParts().add(startfolderPart);

		construct.save();
		t.commit(false);

		return ObjectTransformer.getInt(construct.getId(), 0);
	}
}
