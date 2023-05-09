<?xml version="1.0"?>
<!-- 
	Taken from the daisydiff-1.1 distribution (htmlheader.xsl) and adapted for integration with GCN.
-->
<!--
  Copyright 2004 Guy Van den Broeck

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" indent="yes"/>

<xsl:template match="/">
  <xsl:apply-templates select="diffreport/diff/node()"/>
</xsl:template>

<xsl:template match="@*|node()">
<xsl:copy>
  <xsl:apply-templates select="@*|node()"/>
</xsl:copy>
</xsl:template>

<xsl:template match="img">
<img>
  <xsl:copy-of select="@*"/>
  <xsl:if test="@changeType='diff-removed-image' or @changeType='diff-added-image'">
        <xsl:attribute name="onLoad">updateOverlays()</xsl:attribute>
        <xsl:attribute name="onError">updateOverlays()</xsl:attribute>
        <xsl:attribute name="onAbort">updateOverlays()</xsl:attribute>
  </xsl:if>

</img>
</xsl:template>

<!-- commented-out support for diff onclick handlers since they don't work so well with GCN
     To re-enable them, uncomment the relevant script includes in ContentNodeProcessor.daisyDiff() -->
<!--
<xsl:template match="span[@class='diff-html-changed']">
<span>
  <xsl:copy-of select="@*"/>
  <xsl:attribute name="onclick">return tipC(constructToolTipC(this));</xsl:attribute>
  <xsl:apply-templates select="node()"/>
</span>
</xsl:template>

<xsl:template match="span[@class='diff-html-added']">
<span>
  <xsl:copy-of select="@*"/>
  <xsl:attribute name="onclick">return tipA(constructToolTipA(this));</xsl:attribute>
  <xsl:apply-templates select="node()"/>
</span>
</xsl:template>

<xsl:template match="span[@class='diff-html-removed']">
<span>
  <xsl:copy-of select="@*"/>
  <xsl:attribute name="onclick">return tipR(constructToolTipR(this));</xsl:attribute>
  <xsl:apply-templates select="node()"/>
</span>
</xsl:template>
-->

</xsl:stylesheet>
