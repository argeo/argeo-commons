<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:slc="http://argeo.org/projects/slc/schemas"
	exclude-result-prefixes="slc">

	<xsl:template match="/">
		<xsl:copy-of select="*"/>
	</xsl:template>
</xsl:stylesheet>