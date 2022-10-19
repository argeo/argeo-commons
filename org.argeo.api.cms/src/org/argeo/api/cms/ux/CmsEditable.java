package org.argeo.api.cms.ux;

/** Abstraction of a simple edition life cycle. */
public interface CmsEditable {

	/** Whether the calling thread can edit, the value is immutable */
	Boolean canEdit();

	Boolean isEditing();

	void startEditing();

	void stopEditing();

	void addCmsEditionListener(CmsEditionListener listener);

	void removeCmsEditionListener(CmsEditionListener listener);

	static CmsEditable NON_EDITABLE = new CmsEditable() {

		@Override
		public void stopEditing() {
		}

		@Override
		public void startEditing() {
		}

		@Override
		public Boolean isEditing() {
			return false;
		}

		@Override
		public Boolean canEdit() {
			return false;
		}

		@Override
		public void addCmsEditionListener(CmsEditionListener listener) {
		}

		@Override
		public void removeCmsEditionListener(CmsEditionListener listener) {
		}

	};

	static CmsEditable ALWAYS_EDITING = new CmsEditable() {

		@Override
		public void stopEditing() {
		}

		@Override
		public void startEditing() {
		}

		@Override
		public Boolean isEditing() {
			return true;
		}

		@Override
		public Boolean canEdit() {
			return true;
		}

		@Override
		public void addCmsEditionListener(CmsEditionListener listener) {
		}

		@Override
		public void removeCmsEditionListener(CmsEditionListener listener) {
		}

	};

}
