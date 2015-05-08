package org.argeo.cms.util;

import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.argeo.cms.AbstractCmsEntryPoint;
import org.argeo.cms.CmsException;
import org.argeo.cms.CmsImageManager;
import org.argeo.cms.CmsStyles;
import org.argeo.cms.CmsUiProvider;
import org.argeo.cms.internal.ImageManagerImpl;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/** Simple header/body ergonomics. */
public class SimpleErgonomics extends AbstractCmsEntryPoint {
	private Composite headerArea;
	private Composite bodyArea;
	private final CmsUiProvider uiProvider;

	private CmsUiProvider header;
	private Integer headerHeight = 40;

	private CmsImageManager imageManager = new ImageManagerImpl();

	public SimpleErgonomics(Repository repository, String workspace,
			String defaultPath, CmsUiProvider uiProvider,
			Map<String, String> factoryProperties) {
		super(repository, workspace, defaultPath, factoryProperties);
		this.uiProvider = uiProvider;
	}

	@Override
	protected void createUi(Composite parent) {
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		parent.setLayout(CmsUtils.noSpaceGridLayout());

		headerArea = new Composite(parent, SWT.NONE);
		headerArea.setLayout(new FillLayout());
		GridData headerData = new GridData(SWT.FILL, SWT.FILL, false, false);
		headerData.heightHint = headerHeight;
		headerArea.setLayoutData(headerData);

		bodyArea = new Composite(parent, SWT.NONE);
		bodyArea.setData(RWT.CUSTOM_VARIANT, CmsStyles.CMS_BODY);
		bodyArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		bodyArea.setLayout(CmsUtils.noSpaceGridLayout());

		refresh();
	}

	@Override
	protected void refresh() {
		refreshHeader();
		refreshBody();
	}

	protected void refreshHeader() {
		if (headerArea == null)
			return;
		for (Control child : headerArea.getChildren())
			child.dispose();
		try {
			header.createUi(headerArea, getNode());
		} catch (RepositoryException e) {
			throw new CmsException("Cannot refresh header", e);
		}
		headerArea.layout(true, true);
	}

	protected void refreshBody() {
		if (bodyArea == null)
			return;
		// Exception
		Throwable exception = getException();
		if (exception != null) {
			SystemNotifications systemNotifications = new SystemNotifications(
					bodyArea);
			systemNotifications.notifyException(exception);
			resetException();
			return;
			// TODO report
		}

		// clear
		for (Control child : bodyArea.getChildren())
			child.dispose();
		bodyArea.setLayout(CmsUtils.noSpaceGridLayout());

		String state = getState();
		try {
			if (state == null)
				setState("");
			// throw new CmsException("State cannot be null");
			uiProvider.createUi(bodyArea, getNode());
		} catch (RepositoryException e) {
			throw new CmsException("Cannot refresh body", e);
		}

		bodyArea.layout(true, true);
	}

	@Override
	public CmsImageManager getImageManager() {
		return imageManager;
	}

	public void setHeader(CmsUiProvider header) {
		this.header = header;
	}

	public void setHeaderHeight(Integer headerHeight) {
		this.headerHeight = headerHeight;
	}

	public void setImageManager(CmsImageManager imageManager) {
		this.imageManager = imageManager;
	}
}
