<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:slc="http://argeo.org/projects/slc/schemas"
	exclude-result-prefixes="slc">

	<xsl:output method="html" omit-xml-declaration="yes" />
	

	<xsl:template match="/">
		<html>
			<head>
				<title>Result</title>
			</head>
			<body style="font-family: sans-serif">
				<h1>
					Result
					<xsl:value-of select="slc:tree-test-result/@uuid" />
				</h1>
				<xsl:for-each
					select="slc:tree-test-result/slc:result-parts/slc:result-part">
					<h2>
						<xsl:value-of select="@path" />
					</h2>
					<table>
						<xsl:for-each
							select="slc:part-sub-list/slc:parts/slc:simple-result-part">
							<tr>
								<xsl:choose>
									<xsl:when
										test="slc:status = 'PASSED' ">
										<td style="color:green">
											<xsl:value-of
												select="slc:message" />
										</td>
									</xsl:when>
									<xsl:otherwise>
										<td style="color:red">
											<xsl:value-of
												select="slc:message" />
										</td>
									</xsl:otherwise>
								</xsl:choose>
							</tr>
						</xsl:for-each>
					</table>
				</xsl:for-each>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>