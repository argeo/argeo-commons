package org.argeo.api.cms.ux;

/** Abstraction of a simple edition life cycle. */
public interface CmsEditable {

	/** Whether the calling thread can edit, the value is immutable */
	public Boolean canEdit();

	public Boolean isEditing();

	public void startEditing();

	public void stopEditing();

	public static CmsEditable NON_EDITABLE = new CmsEditable() {

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
	};

	public static CmsEditable ALWAYS_EDITING = new CmsEditable() {

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
	};

}
