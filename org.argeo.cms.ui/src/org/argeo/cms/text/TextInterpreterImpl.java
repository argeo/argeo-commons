package org.argeo.cms.text;

import javax.jcr.Item;
import javax.jcr.RepositoryException;

/**
 * Text interpreter that sanitise and validates before saving, and support CMS
 * specific formatting and integration.
 */
class TextInterpreterImpl extends IdentityTextInterpreter {
	private MarkupValidatorCopy markupValidator = MarkupValidatorCopy
			.getInstance();

	@Override
	protected void validateBeforeStoring(String raw) {
		markupValidator.validate(raw);
	}

	@Override
	protected String convertToStorage(Item item, String content)
			throws RepositoryException {
		return super.convertToStorage(item, content);
	}

	@Override
	protected String convertFromStorage(Item item, String content)
			throws RepositoryException {
		return super.convertFromStorage(item, content);
	}

}
