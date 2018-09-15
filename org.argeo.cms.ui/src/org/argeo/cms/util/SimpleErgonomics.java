package org.argeo.cms.util;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.cms.ui.AbstractCmsEntryPoint;
import org.argeo.cms.ui.CmsImageManager;
import org.argeo.cms.ui.CmsStyles;
import org.argeo.cms.ui.CmsUiProvider;
import org.argeo.cms.ui.UxContext;
import org.argeo.cms.ui.internal.ImageManagerImpl;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/** Simple header/body ergonomics. */
public class SimpleErgonomics extends AbstractCmsEntryPoint {
	private static final long serialVersionUID = 8743413921359548523L;

	private final static Log log = LogFactory.getLog(SimpleErgonomics.class);

	private boolean uiInitialized = false;
	private Composite headerArea;
	private Composite bodyArea;
	private final CmsUiProvider uiProvider;

	private CmsUiProvider header;
	private Integer headerHeight = 0;

	private CmsImageManager imageManager = new ImageManagerImpl();
	private UxContext uxContext = null;

	public SimpleErgonomics(Repository repository, String workspace, String defaultPath, CmsUiProvider uiProvider,
			Map<String, String> factoryProperties) {
		super(repository, workspace, defaultPath, factoryProperties);
		this.uiProvider = uiProvider;
	}

	@Override
	protected void initUi(Composite parent) {
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		parent.setLayout(CmsUtils.noSpaceGridLayout());

		uxContext = new SimpleUxContext();
		if (!getUxContext().isMasterData())
			createAdminArea(parent);
		headerArea = new Composite(parent, SWT.NONE);
		headerArea.setLayout(new FillLayout());
		GridData headerData = new GridData(SWT.FILL, SWT.FILL, false, false);
		headerData.heightHint = headerHeight;
		headerArea.setLayoutData(headerData);

		bodyArea = new Composite(parent, SWT.NONE);
		bodyArea.setData(RWT.CUSTOM_VARIANT, CmsStyles.CMS_BODY);
		bodyArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		bodyArea.setLayout(CmsUtils.noSpaceGridLayout());
		uiInitialized = true;
		refresh();
	}

	@Override
	protected void refresh() {
		if (!uiInitialized)
			return;
		if (getState() == null)
			setState("");
		refreshHeader();
		refreshBody();
		if (log.isTraceEnabled())
			log.trace("UI refreshed " + getNode());
	}

	protected void createAdminArea(Composite parent) {
	}

	protected void refreshHeader() {
		if (header == null)
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
		// Exception
		Throwable exception = getException();
		if (exception != null) {
			SystemNotifications systemNotifications = new SystemNotifications(bodyArea);
			systemNotifications.notifyException(exception);
			resetException();
			return;
			// TODO report
		}

		// clear
		for (Control child : bodyArea.getChildren())
			child.dispose();
		bodyArea.setLayout(CmsUtils.noSpaceGridLayout());

		try {
			Node node = getNode();
			if (node == null)
				log.error("Context cannot be null");
			else
				uiProvider.createUi(bodyArea, node);
		} catch (RepositoryException e) {
			throw new CmsException("Cannot refresh body", e);
		}

		bodyArea.layout(true, true);
	}

	@Override
	public UxContext getUxContext() {
		return uxContext;
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
