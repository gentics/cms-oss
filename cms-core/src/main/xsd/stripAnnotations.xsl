<?xml version="1.0"?>

<!--
 Copyright 2003 Sun Microsystems, Inc. All rights reserved.
-->

<xsl:stylesheet
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:jaxb="http://java.sun.com/xml/ns/jaxb"
	xmlns:xs="http://www.w3.org/2001/XMLSchema">
	
	<xsl:template match="/">
		<xsl:apply-templates />
	</xsl:template>

	<!-- ignore annotation -->
	<xsl:template match="xs:annotation"/>
	
	<xsl:template match="/xs:schema">
		<xs:schema xmlns="http://www.gentics.com/xml/ns/portal/view"
			elementFormDefault="qualified" attributeFormDefault="unqualified"
			targetNamespace="http://www.gentics.com/xml/ns/portal/view"
			xmlns:xs="http://www.w3.org/2001/XMLSchema"
			xmlns:jaxb="http://java.sun.com/xml/ns/jaxb"
			jaxb:version="1.0">
			<xsl:apply-templates />
		</xs:schema>
	</xsl:template>
	<xsl:template match="/xs:schema/xs:element">
		<xs:element name="view" type="view" />
	</xsl:template>
	
	<xsl:template match="*">
		<xsl:copy>
			<xsl:apply-templates select="@*|*|text()|comment()|processing-instruction()" />
		</xsl:copy>
	</xsl:template>

	<xsl:template match="@*|comment()|processing-instruction()">
		<xsl:copy-of select="."/>
	</xsl:template>

	<xsl:template match="xs:attribute">
		<xsl:choose>
			<xsl:when test="contains( comment(), '@deprecated' )">
				<xsl:message>Deprecated attribute <xsl:value-of select="../@name" /> / <xsl:value-of select="@name" /></xsl:message>
			</xsl:when>
			<xsl:otherwise>
				<xsl:copy>
					<xsl:apply-templates select="@*|*|text()|comment()|processing-instruction()" />
					<xsl:if test="/xs:schema/xs:complexType[@name=current()/@type]/xs:annotation/xs:documentation">
						<xs:annotation>
							<xsl:copy-of select="/xs:schema/xs:complexType[@name=current()/@type]/xs:annotation/xs:documentation" />
						</xs:annotation>
					</xsl:if>
				</xsl:copy>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="xs:element">
		<xsl:choose>
			<xsl:when test="contains( /xs:schema/xs:complexType[@name=current()/@type]/comment(), '@deprecated' )">
				<xsl:message>Deprecated element - reference .. <xsl:value-of select="@type" /></xsl:message>
			</xsl:when>
			<xsl:when test="contains( comment(), '@deprecated' )">
				<xsl:message>Deprecated element <xsl:value-of select="../../@name" /> / <xsl:value-of select="@name" /></xsl:message>
			</xsl:when>
			<xsl:otherwise>
				<xsl:copy>
					<xsl:apply-templates select="@*|*|text()|comment()|processing-instruction()" />
					<xsl:if test="/xs:schema/xs:complexType[@name=current()/@type]/xs:annotation/xs:documentation">
						<xs:annotation>
							<xsl:copy-of select="/xs:schema/xs:complexType[@name=current()/@type]/xs:annotation/xs:documentation" />
						</xs:annotation>
					</xsl:if>
				</xsl:copy>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="xs:complexType|xs:simpleType">
		<xsl:choose>
			<xsl:when test="contains( comment(), '@deprecated' )">
				<xsl:message>Deprecated type: <xsl:value-of select="@name" /></xsl:message>
			</xsl:when>
			<xsl:otherwise>
				<xsl:copy>
					<xsl:apply-templates select="@*|*|text()|comment()|processing-instruction()" />
				</xsl:copy>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
</xsl:stylesheet>
