<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:template match="/">
<html>
	<head>
		<xsl:apply-templates select="view/header/title"/>
	</head>
	<body>
		<xsl:apply-templates select="view/body/form"/>
	</body>
</html>
</xsl:template>
<xsl:template match="view/header/title">
	<xsl:element name="title">
		<xsl:value-of select="."/>
	</xsl:element>
</xsl:template>
<xsl:template match="view/body/form">
	<xsl:element name="form">
		<xsl:attribute name="name">
		    <xsl:value-of select="name"/>
		    </xsl:attribute>
		<xsl:attribute name="action">
		    <xsl:value-of select="action"/>
		    </xsl:attribute>
		<xsl:attribute name="method">
			<xsl:value-of select="method"/>
			</xsl:attribute>
		<table align="center">
		<xsl:apply-templates select="./textView"/>
		<tr>
		 <td></td><td align="right"><xsl:apply-templates select="./buttonView"/></td>
		</tr>
		</table>
	</xsl:element>
</xsl:template>
<xsl:template match="view/body/form/textView">
    <br/>
	<tr>
		<td align="right">
			<xsl:value-of select="label"/>
		</td>
		<td align="right">
			<xsl:element name="input">
			<xsl:attribute name="name">
				<xsl:value-of select="name"/>
			</xsl:attribute>
			<xsl:attribute name="value">
				<xsl:value-of select="value"/>
			</xsl:attribute>
			</xsl:element>
		</td>
	</tr>
</xsl:template>
<xsl:template match="view/body/form/buttonView">
    <br/>
	<xsl:element name="input">
		<xsl:attribute name="type">
			<xsl:value-of select="type" />
	    </xsl:attribute>
		<xsl:attribute name="name">
			<xsl:value-of select="name" />
		</xsl:attribute>
		<xsl:attribute name="value">
			<xsl:value-of select="label" />
		</xsl:attribute>
	</xsl:element>
</xsl:template>
</xsl:stylesheet>