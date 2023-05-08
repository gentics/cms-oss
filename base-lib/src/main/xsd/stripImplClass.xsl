<?xml version="1.0"?>

<!--
 Copyright 2003 Sun Microsystems, Inc. All rights reserved.
-->

<xsl:stylesheet
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:jaxb="http://java.sun.com/xml/ns/jaxb">
	
	<xsl:template match="/">
		<xsl:apply-templates />
	</xsl:template>
	
	<xsl:template match="*">
		<xsl:copy>
			<xsl:apply-templates select="@*|*|text()|comment()|processing-instruction()" />
		</xsl:copy>
	</xsl:template>
	
	<!-- ignore @implClass -->
	<xsl:template match="@implClass[parent::jaxb:class]"/>
	
	<xsl:template match="@*|comment()|processing-instruction()">
		<xsl:copy-of select="."/>
	</xsl:template>
	
	<!--xsl:template name="test">
		<foo c="d">ab<bar a="b" implClass="5"/>cd</foo>
		<jaxb:class implClass="foO" />
	</xsl:template-->
</xsl:stylesheet>
