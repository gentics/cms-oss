//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.4-hudson-16-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.04.24 at 01:20:39 CEST 
//


package com.gentics.contentnode.dbcopy.jaxb.impl;

public class JAXBreferencesTypeImpl implements com.gentics.contentnode.dbcopy.jaxb.JAXBreferencesType, com.sun.xml.bind.JAXBObject, com.gentics.contentnode.dbcopy.jaxb.impl.runtime.UnmarshallableObject, com.gentics.contentnode.dbcopy.jaxb.impl.runtime.XMLSerializable, com.gentics.contentnode.dbcopy.jaxb.impl.runtime.ValidatableObject
{

    protected com.sun.xml.bind.util.ListImpl _Ref;
    public final static java.lang.Class version = (com.gentics.contentnode.dbcopy.jaxb.impl.JAXBVersion.class);
    private static com.sun.msv.grammar.Grammar schemaFragment;

    private final static java.lang.Class PRIMARY_INTERFACE_CLASS() {
        return (com.gentics.contentnode.dbcopy.jaxb.JAXBreferencesType.class);
    }

    protected com.sun.xml.bind.util.ListImpl _getRef() {
        if (_Ref == null) {
            _Ref = new com.sun.xml.bind.util.ListImpl(new java.util.ArrayList());
        }
        return _Ref;
    }

    public com.gentics.contentnode.dbcopy.jaxb.JAXBreferenceType[] getRef() {
        if (_Ref == null) {
            return new com.gentics.contentnode.dbcopy.jaxb.JAXBreferenceType[ 0 ] ;
        }
        return ((com.gentics.contentnode.dbcopy.jaxb.JAXBreferenceType[]) _Ref.toArray(new com.gentics.contentnode.dbcopy.jaxb.JAXBreferenceType[_Ref.size()] ));
    }

    public com.gentics.contentnode.dbcopy.jaxb.JAXBreferenceType getRef(int idx) {
        if (_Ref == null) {
            throw new java.lang.IndexOutOfBoundsException();
        }
        return ((com.gentics.contentnode.dbcopy.jaxb.JAXBreferenceType) _Ref.get(idx));
    }

    public int getRefLength() {
        if (_Ref == null) {
            return  0;
        }
        return _Ref.size();
    }

    public void setRef(com.gentics.contentnode.dbcopy.jaxb.JAXBreferenceType[] values) {
        _getRef().clear();
        int len = values.length;
        for (int i = 0; (i<len); i ++) {
            _Ref.add(values[i]);
        }
    }

    public com.gentics.contentnode.dbcopy.jaxb.JAXBreferenceType setRef(int idx, com.gentics.contentnode.dbcopy.jaxb.JAXBreferenceType value) {
        return ((com.gentics.contentnode.dbcopy.jaxb.JAXBreferenceType) _Ref.set(idx, value));
    }

    public boolean isSetRef() {
        return ((_Ref == null)?false:_Ref.isModified());
    }

    public void unsetRef() {
        if (_Ref!= null) {
            _Ref.clear();
            _Ref.setModified(false);
        }
    }

    public com.gentics.contentnode.dbcopy.jaxb.impl.runtime.UnmarshallingEventHandler createUnmarshaller(com.gentics.contentnode.dbcopy.jaxb.impl.runtime.UnmarshallingContext context) {
        return new com.gentics.contentnode.dbcopy.jaxb.impl.JAXBreferencesTypeImpl.Unmarshaller(context);
    }

    public void serializeBody(com.gentics.contentnode.dbcopy.jaxb.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx1 = 0;
        final int len1 = ((_Ref == null)? 0 :_Ref.size());
        while (idx1 != len1) {
            context.startElement("", "ref");
            int idx_0 = idx1;
            context.childAsURIs(((com.sun.xml.bind.JAXBObject) _Ref.get(idx_0 ++)), "Ref");
            context.endNamespaceDecls();
            int idx_1 = idx1;
            context.childAsAttributes(((com.sun.xml.bind.JAXBObject) _Ref.get(idx_1 ++)), "Ref");
            context.endAttributes();
            context.childAsBody(((com.sun.xml.bind.JAXBObject) _Ref.get(idx1 ++)), "Ref");
            context.endElement();
        }
    }

    public void serializeAttributes(com.gentics.contentnode.dbcopy.jaxb.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx1 = 0;
        final int len1 = ((_Ref == null)? 0 :_Ref.size());
        while (idx1 != len1) {
            idx1 += 1;
        }
    }

    public void serializeURIs(com.gentics.contentnode.dbcopy.jaxb.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx1 = 0;
        final int len1 = ((_Ref == null)? 0 :_Ref.size());
        while (idx1 != len1) {
            idx1 += 1;
        }
    }

    public java.lang.Class getPrimaryInterface() {
        return (com.gentics.contentnode.dbcopy.jaxb.JAXBreferencesType.class);
    }

    public com.sun.msv.verifier.DocumentDeclaration createRawValidator() {
        if (schemaFragment == null) {
            schemaFragment = com.sun.xml.bind.validator.SchemaDeserializer.deserialize((
 "\u00ac\u00ed\u0000\u0005sr\u0000\u001dcom.sun.msv.grammar.ChoiceExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000\u001dcom.sun."
+"msv.grammar.BinaryExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002L\u0000\u0004exp1t\u0000 Lcom/sun/msv/gramm"
+"ar/Expression;L\u0000\u0004exp2q\u0000~\u0000\u0002xr\u0000\u001ecom.sun.msv.grammar.Expression"
+"\u00f8\u0018\u0082\u00e8N5~O\u0002\u0000\u0002L\u0000\u0013epsilonReducibilityt\u0000\u0013Ljava/lang/Boolean;L\u0000\u000bex"
+"pandedExpq\u0000~\u0000\u0002xpppsr\u0000 com.sun.msv.grammar.OneOrMoreExp\u0000\u0000\u0000\u0000\u0000\u0000"
+"\u0000\u0001\u0002\u0000\u0000xr\u0000\u001ccom.sun.msv.grammar.UnaryExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001L\u0000\u0003expq\u0000~\u0000\u0002x"
+"q\u0000~\u0000\u0003sr\u0000\u0011java.lang.Boolean\u00cd r\u0080\u00d5\u009c\u00fa\u00ee\u0002\u0000\u0001Z\u0000\u0005valuexp\u0000psr\u0000\'com.sun"
+".msv.grammar.trex.ElementPattern\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001L\u0000\tnameClasst\u0000\u001fLc"
+"om/sun/msv/grammar/NameClass;xr\u0000\u001ecom.sun.msv.grammar.Element"
+"Exp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002Z\u0000\u001aignoreUndeclaredAttributesL\u0000\fcontentModelq\u0000"
+"~\u0000\u0002xq\u0000~\u0000\u0003q\u0000~\u0000\np\u0000sr\u0000\u001fcom.sun.msv.grammar.SequenceExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002"
+"\u0000\u0000xq\u0000~\u0000\u0001ppsq\u0000~\u0000\u000bpp\u0000sq\u0000~\u0000\u0000ppsq\u0000~\u0000\u0006q\u0000~\u0000\npsr\u0000 com.sun.msv.gramm"
+"ar.AttributeExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002L\u0000\u0003expq\u0000~\u0000\u0002L\u0000\tnameClassq\u0000~\u0000\fxq\u0000~\u0000\u0003"
+"q\u0000~\u0000\npsr\u00002com.sun.msv.grammar.Expression$AnyStringExpression"
+"\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0003sq\u0000~\u0000\t\u0001q\u0000~\u0000\u0017sr\u0000 com.sun.msv.grammar.AnyName"
+"Class\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000\u001dcom.sun.msv.grammar.NameClass\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000"
+"xpsr\u00000com.sun.msv.grammar.Expression$EpsilonExpression\u0000\u0000\u0000\u0000\u0000\u0000"
+"\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0003q\u0000~\u0000\u0018q\u0000~\u0000\u001dsr\u0000#com.sun.msv.grammar.SimpleNameClass"
+"\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002L\u0000\tlocalNamet\u0000\u0012Ljava/lang/String;L\u0000\fnamespaceURIq"
+"\u0000~\u0000\u001fxq\u0000~\u0000\u001at\u00005com.gentics.contentnode.dbcopy.jaxb.JAXBreferen"
+"ceTypet\u0000+http://java.sun.com/jaxb/xjc/dummy-elementssq\u0000~\u0000\u0000pp"
+"sq\u0000~\u0000\u0014q\u0000~\u0000\npsr\u0000\u001bcom.sun.msv.grammar.DataExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0003L\u0000\u0002dtt"
+"\u0000\u001fLorg/relaxng/datatype/Datatype;L\u0000\u0006exceptq\u0000~\u0000\u0002L\u0000\u0004namet\u0000\u001dLco"
+"m/sun/msv/util/StringPair;xq\u0000~\u0000\u0003ppsr\u0000\"com.sun.msv.datatype.x"
+"sd.QnameType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000*com.sun.msv.datatype.xsd.BuiltinA"
+"tomicType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000%com.sun.msv.datatype.xsd.ConcreteTyp"
+"e\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000\'com.sun.msv.datatype.xsd.XSDatatypeImpl\u0000\u0000\u0000\u0000\u0000"
+"\u0000\u0000\u0001\u0002\u0000\u0003L\u0000\fnamespaceUriq\u0000~\u0000\u001fL\u0000\btypeNameq\u0000~\u0000\u001fL\u0000\nwhiteSpacet\u0000.Lc"
+"om/sun/msv/datatype/xsd/WhiteSpaceProcessor;xpt\u0000 http://www."
+"w3.org/2001/XMLSchemat\u0000\u0005QNamesr\u00005com.sun.msv.datatype.xsd.Wh"
+"iteSpaceProcessor$Collapse\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000,com.sun.msv.datatyp"
+"e.xsd.WhiteSpaceProcessor\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xpsr\u00000com.sun.msv.gramma"
+"r.Expression$NullSetExpression\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0003ppsr\u0000\u001bcom.sun"
+".msv.util.StringPair\u00d0t\u001ejB\u008f\u008d\u00a0\u0002\u0000\u0002L\u0000\tlocalNameq\u0000~\u0000\u001fL\u0000\fnamespace"
+"URIq\u0000~\u0000\u001fxpq\u0000~\u00000q\u0000~\u0000/sq\u0000~\u0000\u001et\u0000\u0004typet\u0000)http://www.w3.org/2001/X"
+"MLSchema-instanceq\u0000~\u0000\u001dsq\u0000~\u0000\u001et\u0000\u0003reft\u0000\u0000q\u0000~\u0000\u001dsr\u0000\"com.sun.msv.gr"
+"ammar.ExpressionPool\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001L\u0000\bexpTablet\u0000/Lcom/sun/msv/gr"
+"ammar/ExpressionPool$ClosedHash;xpsr\u0000-com.sun.msv.grammar.Ex"
+"pressionPool$ClosedHash\u00d7j\u00d0N\u00ef\u00e8\u00ed\u001c\u0003\u0000\u0003I\u0000\u0005countB\u0000\rstreamVersionL\u0000"
+"\u0006parentt\u0000$Lcom/sun/msv/grammar/ExpressionPool;xp\u0000\u0000\u0000\u0006\u0001pq\u0000~\u0000\u0012q"
+"\u0000~\u0000#q\u0000~\u0000\bq\u0000~\u0000\u0013q\u0000~\u0000\u0010q\u0000~\u0000\u0005x"));
        }
        return new com.sun.msv.verifier.regexp.REDocumentDeclaration(schemaFragment);
    }

    public class Unmarshaller
        extends com.gentics.contentnode.dbcopy.jaxb.impl.runtime.AbstractUnmarshallingEventHandlerImpl
    {


        public Unmarshaller(com.gentics.contentnode.dbcopy.jaxb.impl.runtime.UnmarshallingContext context) {
            super(context, "----");
        }

        protected Unmarshaller(com.gentics.contentnode.dbcopy.jaxb.impl.runtime.UnmarshallingContext context, int startState) {
            this(context);
            state = startState;
        }

        public java.lang.Object owner() {
            return com.gentics.contentnode.dbcopy.jaxb.impl.JAXBreferencesTypeImpl.this;
        }

        public void enterElement(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname, org.xml.sax.Attributes __atts)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  0 :
                        if (("ref" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, false);
                            state = 1;
                            return ;
                        }
                        state = 3;
                        continue outer;
                    case  3 :
                        if (("ref" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, false);
                            state = 1;
                            return ;
                        }
                        revertToParentFromEnterElement(___uri, ___local, ___qname, __atts);
                        return ;
                    case  1 :
                        attIdx = context.getAttribute("", "class");
                        if (attIdx >= 0) {
                            context.consumeAttribute(attIdx);
                            context.getCurrentHandler().enterElement(___uri, ___local, ___qname, __atts);
                            return ;
                        }
                        attIdx = context.getAttribute("", "col");
                        if (attIdx >= 0) {
                            context.consumeAttribute(attIdx);
                            context.getCurrentHandler().enterElement(___uri, ___local, ___qname, __atts);
                            return ;
                        }
                        attIdx = context.getAttribute("", "deepcopy");
                        if (attIdx >= 0) {
                            context.consumeAttribute(attIdx);
                            context.getCurrentHandler().enterElement(___uri, ___local, ___qname, __atts);
                            return ;
                        }
                        attIdx = context.getAttribute("", "foreigndeepcopy");
                        if (attIdx >= 0) {
                            context.consumeAttribute(attIdx);
                            context.getCurrentHandler().enterElement(___uri, ___local, ___qname, __atts);
                            return ;
                        }
                        attIdx = context.getAttribute("", "target");
                        if (attIdx >= 0) {
                            context.consumeAttribute(attIdx);
                            context.getCurrentHandler().enterElement(___uri, ___local, ___qname, __atts);
                            return ;
                        }
                        if (("parameter" == ___local)&&("" == ___uri)) {
                            _getRef().add(((com.gentics.contentnode.dbcopy.Reference) spawnChildFromEnterElement((com.gentics.contentnode.dbcopy.Reference.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        _getRef().add(((com.gentics.contentnode.dbcopy.Reference) spawnChildFromEnterElement((com.gentics.contentnode.dbcopy.Reference.class), 2, ___uri, ___local, ___qname, __atts)));
                        return ;
                }
                super.enterElement(___uri, ___local, ___qname, __atts);
                break;
            }
        }

        public void leaveElement(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  0 :
                        state = 3;
                        continue outer;
                    case  3 :
                        revertToParentFromLeaveElement(___uri, ___local, ___qname);
                        return ;
                    case  1 :
                        attIdx = context.getAttribute("", "class");
                        if (attIdx >= 0) {
                            context.consumeAttribute(attIdx);
                            context.getCurrentHandler().leaveElement(___uri, ___local, ___qname);
                            return ;
                        }
                        attIdx = context.getAttribute("", "col");
                        if (attIdx >= 0) {
                            context.consumeAttribute(attIdx);
                            context.getCurrentHandler().leaveElement(___uri, ___local, ___qname);
                            return ;
                        }
                        attIdx = context.getAttribute("", "deepcopy");
                        if (attIdx >= 0) {
                            context.consumeAttribute(attIdx);
                            context.getCurrentHandler().leaveElement(___uri, ___local, ___qname);
                            return ;
                        }
                        attIdx = context.getAttribute("", "foreigndeepcopy");
                        if (attIdx >= 0) {
                            context.consumeAttribute(attIdx);
                            context.getCurrentHandler().leaveElement(___uri, ___local, ___qname);
                            return ;
                        }
                        attIdx = context.getAttribute("", "target");
                        if (attIdx >= 0) {
                            context.consumeAttribute(attIdx);
                            context.getCurrentHandler().leaveElement(___uri, ___local, ___qname);
                            return ;
                        }
                        _getRef().add(((com.gentics.contentnode.dbcopy.Reference) spawnChildFromLeaveElement((com.gentics.contentnode.dbcopy.Reference.class), 2, ___uri, ___local, ___qname)));
                        return ;
                    case  2 :
                        if (("ref" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 3;
                            return ;
                        }
                        break;
                }
                super.leaveElement(___uri, ___local, ___qname);
                break;
            }
        }

        public void enterAttribute(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  0 :
                        state = 3;
                        continue outer;
                    case  3 :
                        revertToParentFromEnterAttribute(___uri, ___local, ___qname);
                        return ;
                    case  1 :
                        if (("class" == ___local)&&("" == ___uri)) {
                            _getRef().add(((com.gentics.contentnode.dbcopy.Reference) spawnChildFromEnterAttribute((com.gentics.contentnode.dbcopy.Reference.class), 2, ___uri, ___local, ___qname)));
                            return ;
                        }
                        if (("col" == ___local)&&("" == ___uri)) {
                            _getRef().add(((com.gentics.contentnode.dbcopy.Reference) spawnChildFromEnterAttribute((com.gentics.contentnode.dbcopy.Reference.class), 2, ___uri, ___local, ___qname)));
                            return ;
                        }
                        if (("deepcopy" == ___local)&&("" == ___uri)) {
                            _getRef().add(((com.gentics.contentnode.dbcopy.Reference) spawnChildFromEnterAttribute((com.gentics.contentnode.dbcopy.Reference.class), 2, ___uri, ___local, ___qname)));
                            return ;
                        }
                        if (("foreigndeepcopy" == ___local)&&("" == ___uri)) {
                            _getRef().add(((com.gentics.contentnode.dbcopy.Reference) spawnChildFromEnterAttribute((com.gentics.contentnode.dbcopy.Reference.class), 2, ___uri, ___local, ___qname)));
                            return ;
                        }
                        if (("target" == ___local)&&("" == ___uri)) {
                            _getRef().add(((com.gentics.contentnode.dbcopy.Reference) spawnChildFromEnterAttribute((com.gentics.contentnode.dbcopy.Reference.class), 2, ___uri, ___local, ___qname)));
                            return ;
                        }
                        _getRef().add(((com.gentics.contentnode.dbcopy.Reference) spawnChildFromEnterAttribute((com.gentics.contentnode.dbcopy.Reference.class), 2, ___uri, ___local, ___qname)));
                        return ;
                }
                super.enterAttribute(___uri, ___local, ___qname);
                break;
            }
        }

        public void leaveAttribute(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  0 :
                        state = 3;
                        continue outer;
                    case  3 :
                        revertToParentFromLeaveAttribute(___uri, ___local, ___qname);
                        return ;
                    case  1 :
                        attIdx = context.getAttribute("", "class");
                        if (attIdx >= 0) {
                            context.consumeAttribute(attIdx);
                            context.getCurrentHandler().leaveAttribute(___uri, ___local, ___qname);
                            return ;
                        }
                        attIdx = context.getAttribute("", "col");
                        if (attIdx >= 0) {
                            context.consumeAttribute(attIdx);
                            context.getCurrentHandler().leaveAttribute(___uri, ___local, ___qname);
                            return ;
                        }
                        attIdx = context.getAttribute("", "deepcopy");
                        if (attIdx >= 0) {
                            context.consumeAttribute(attIdx);
                            context.getCurrentHandler().leaveAttribute(___uri, ___local, ___qname);
                            return ;
                        }
                        attIdx = context.getAttribute("", "foreigndeepcopy");
                        if (attIdx >= 0) {
                            context.consumeAttribute(attIdx);
                            context.getCurrentHandler().leaveAttribute(___uri, ___local, ___qname);
                            return ;
                        }
                        attIdx = context.getAttribute("", "target");
                        if (attIdx >= 0) {
                            context.consumeAttribute(attIdx);
                            context.getCurrentHandler().leaveAttribute(___uri, ___local, ___qname);
                            return ;
                        }
                        _getRef().add(((com.gentics.contentnode.dbcopy.Reference) spawnChildFromLeaveAttribute((com.gentics.contentnode.dbcopy.Reference.class), 2, ___uri, ___local, ___qname)));
                        return ;
                }
                super.leaveAttribute(___uri, ___local, ___qname);
                break;
            }
        }

        public void handleText(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                try {
                    switch (state) {
                        case  0 :
                            state = 3;
                            continue outer;
                        case  3 :
                            revertToParentFromText(value);
                            return ;
                        case  1 :
                            attIdx = context.getAttribute("", "class");
                            if (attIdx >= 0) {
                                context.consumeAttribute(attIdx);
                                context.getCurrentHandler().text(value);
                                return ;
                            }
                            attIdx = context.getAttribute("", "col");
                            if (attIdx >= 0) {
                                context.consumeAttribute(attIdx);
                                context.getCurrentHandler().text(value);
                                return ;
                            }
                            attIdx = context.getAttribute("", "deepcopy");
                            if (attIdx >= 0) {
                                context.consumeAttribute(attIdx);
                                context.getCurrentHandler().text(value);
                                return ;
                            }
                            attIdx = context.getAttribute("", "foreigndeepcopy");
                            if (attIdx >= 0) {
                                context.consumeAttribute(attIdx);
                                context.getCurrentHandler().text(value);
                                return ;
                            }
                            attIdx = context.getAttribute("", "target");
                            if (attIdx >= 0) {
                                context.consumeAttribute(attIdx);
                                context.getCurrentHandler().text(value);
                                return ;
                            }
                            _getRef().add(((com.gentics.contentnode.dbcopy.Reference) spawnChildFromText((com.gentics.contentnode.dbcopy.Reference.class), 2, value)));
                            return ;
                    }
                } catch (java.lang.RuntimeException e) {
                    handleUnexpectedTextException(value, e);
                }
                break;
            }
        }

    }

}
