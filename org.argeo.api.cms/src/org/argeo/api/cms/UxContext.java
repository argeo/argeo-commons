package org.argeo.api.cms;

public interface UxContext {
	boolean isPortrait();

	boolean isLandscape();

	boolean isSquare();

	boolean isSmall();

	/**
	 * Is a production environment (must be false by default, and be explicitly
	 * set during the CMS deployment). When false, it can activate additional UI
	 * capabilities in order to facilitate QA.
	 */
	boolean isMasterData();
}
