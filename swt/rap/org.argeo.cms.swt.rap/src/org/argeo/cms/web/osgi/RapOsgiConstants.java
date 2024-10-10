package org.argeo.cms.web.osgi;

/**
 * Constants used by RAP v4 within an OSGi environment, especially for
 * registration.
 */
public interface RapOsgiConstants {
	// HttpService / ApplicationConfiguration matching
	// see org.eclipse.rap.rwt.osgi.internal.Matcher in org.eclipse.rap.rwt.osgi
	String HTTP_SERVICE_TARGET = "httpService.target";
	String APPLICATION_CONFIGURATION_TARGET = "applicationConfiguration.target";
	String HTTP_SERVICE_ENDPOINT = "osgi.http.endpoint";
}
