<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="">
	<xsl:template match="action">
		<root>
			<item>
				<content>
					<!-- icon="action.png" -->
					<name>Action '<xsl:value-of select="@name" />' on '<xsl:value-of select="@obj_type" />.<xsl:value-of select="@obj_id" /> <xsl:if test="@obj_cnt" >(+ <xsl:value-of select="@obj_cnt" />)</xsl:if>'</name>
				</content>
				<xsl:apply-templates select="*"/>
			</item>
		</root>
	</xsl:template>
	<xsl:template match="event">
		<item>
			<content>
				<!-- icon="event.png" -->
				<name>Event '<xsl:value-of select="@mask" />' on '<xsl:value-of select="@obj_type" />.<xsl:value-of select="@obj_id" />'<xsl:choose>
						<xsl:when test="@ele_id">|'<xsl:value-of select="@ele_type"/>.<xsl:value-of select="@ele_id"/>'</xsl:when>
						<xsl:when test="@ele_type">|'<xsl:value-of select="@ele_type"/>'</xsl:when>
					</xsl:choose>
					<xsl:if test="@prop"> (property '<xsl:value-of select="@prop"/>')</xsl:if>
				</name>
			</content>
			<xsl:apply-templates select="event|dependency|dirt"/>
		</item>
	</xsl:template>
	<xsl:template match="dependency">
		<item>
			<content>
				<!-- icon="dependency.png" -->
				<name>Dependency for '<xsl:value-of select="@dep_obj_type"/>.<xsl:value-of select="@dep_obj_id"/>'<xsl:choose>
						<xsl:when test="@dep_ele_id">|'<xsl:value-of select="@dep_ele_type"/>.<xsl:value-of select="@dep_ele_id"/>'</xsl:when>
						<xsl:when test="@dep_ele_type">|'<xsl:value-of select="@dep_ele_type"/>'</xsl:when>
					</xsl:choose>
					on
					'<xsl:value-of select="@mod_obj_type"/>.<xsl:value-of select="@mod_obj_id"/>'<xsl:choose>
						<xsl:when test="@mod_ele_id">|'<xsl:value-of select="@mod_ele_type"/>.<xsl:value-of select="@mod_ele_id"/>'</xsl:when>
						<xsl:when test="@mod_ele_type">|'<xsl:value-of select="@mod_ele_type"/>'</xsl:when>
					</xsl:choose>
					<xsl:if test="@mod_prop"> (property '<xsl:value-of select="@mod_prop"/>')</xsl:if>
					for '<xsl:value-of select="@mask" />'.
				</name>
			</content>
			<item hasChildren="true">
				<xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute>
				<content><name>Dependency Details</name></content>
			</item>
			<xsl:apply-templates select="event|dirt"/>
		</item>
	</xsl:template>
	<xsl:template match="dirt">
		<item>
			<content>
				<name>Dirt Object '<xsl:value-of select="@obj_type"/>.<xsl:value-of select="@obj_id"/>'</name>
			</content>
		</item>
	</xsl:template>
</xsl:stylesheet>
