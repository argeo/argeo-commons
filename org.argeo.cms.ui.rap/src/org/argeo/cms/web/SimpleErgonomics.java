package org.argeo.cms.web;

import java.util.Map;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.cms.ui.CmsImageManager;
import org.argeo.cms.ui.CmsStyles;
import org.argeo.cms.ui.CmsUiProvider;
import org.argeo.cms.ui.UxContext;
import org.argeo.cms.ui.util.CmsUiUtils;
import org.argeo.cms.ui.util.DefaultImageManager;
import org.argeo.cms.ui.util.SimpleUxContext;
import org.argeo.cms.ui.util.SystemNotifications;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/** Simple header/body ergonomics. */
@Deprecated
public class SimpleErgonomics extends AbstractCmsEntryPoint {
	private static final long serialVersionUID = 8743413921359548523L;

	private final static Log log = LogFactory.getLog(SimpleErgonomics.class);

	private boolean uiInitialized = false;
	private Composite headerArea;
	private Composite leftArea;
	private Composite rightArea;
	private Composite footerArea;
	private Composite bodyArea;
	private final CmsUiProvider uiProvider;

	private CmsUiProvider header;
	private Integer headerHeight = 0;
	private Integer footerHeight = 0;
	private CmsUiProvider lead;
	private CmsUiProvider end;
	private CmsUiProvider footer;

	private CmsImageManager imageManager = new DefaultImageManager();
	private UxContext uxContext = null;
	private String uid;

	public SimpleErgonomics(Repository repository, String workspace, String defaultPath, CmsUiProvider uiProvider,
			Map<String, String> factoryProperties) {
		super(repository, workspace, defaultPath, factoryProperties);
		this.uiProvider = uiProvider;
	}

	@Override
	protected void initUi(Composite parent) {
		uid = UUID.randomUUID().toString();
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		parent.setLayout(CmsUiUtils.noSpaceGridLayout(new GridLayout(3, false)));

		uxContext = new SimpleUxContext();
		if (!getUxContext().isMasterData())
			createAdminArea(parent);
		headerArea = new Composite(parent, SWT.NONE);
		headerArea.setLayout(new FillLayout());
		GridData headerData = new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1);
		headerData.heightHint = headerHeight;
		headerArea.setLayoutData(headerData);

		// TODO: bi-directional
		leftArea = new Composite(parent, SWT.NONE);
		leftArea.setLayoutData(new GridData(SWT.LEAD, SWT.TOP, false, false));
		leftArea.setLayout(CmsUiUtils.noSpaceGridLayout());

		bodyArea = new Composite(parent, SWT.NONE);
		bodyArea.setData(RWT.CUSTOM_VARIANT, CmsStyles.CMS_BODY);
		bodyArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		bodyArea.setLayout(CmsUiUtils.noSpaceGridLayout());

		// TODO: bi-directional
		rightArea = new Composite(parent, SWT.NONE);
		rightArea.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false));
		rightArea.setLayout(CmsUiUtils.noSpaceGridLayout());

		footerArea = new Composite(parent, SWT.NONE);
		// footerArea.setLayout(new FillLayout());
		GridData footerData = new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1);
		footerData.heightHint = footerHeight;
		footerArea.setLayoutData(footerData);

		uiInitialized = true;
		refresh();
	}

	@Override
	protected void refresh() {
		if (!uiInitialized)
			return;
		if (getState() == null)
			setState("");
		refreshSides();
		refreshBody();
		if (log.isTraceEnabled())
			log.trace("UI refreshed " + getNode());
	}

	protected void createAdminArea(Composite parent) {
	}

	@Deprecated
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

	protected void refreshSides() {
		refresh(headerArea, header, CmsStyles.CMS_HEADER);
		refresh(leftArea, lead, CmsStyles.CMS_LEAD);
		refresh(rightArea, end, CmsStyles.CMS_END);
		refresh(footerArea, footer, CmsStyles.CMS_FOOTER);
	}

	private void refresh(Composite area, CmsUiProvider uiProvider, String style) {
		if (uiProvider == null)
			return;

		for (Control child : area.getChildren())
			child.dispose();
		CmsUiUtils.style(area, style);
		try {
			uiProvider.createUi(area, getNode());
		} catch (RepositoryException e) {
			throw new CmsException("Cannot refresh header", e);
		}
		area.layout(true, true);
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
		bodyArea.setLayout(CmsUiUtils.noSpaceGridLayout());

		try {
			Node node = getNode();
//			if (node == null)
//				log.error("Context cannot be null");
//			else
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
	public String getUid() {
		return uid;
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

	public CmsUiProvider getLead() {
		return lead;
	}

	public void setLead(CmsUiProvider lead) {
		this.lead = lead;
	}

	public CmsUiProvider getEnd() {
		return end;
	}

	public void setEnd(CmsUiProvider end) {
		this.end = end;
	}

	public CmsUiProvider getFooter() {
		return footer;
	}

	public void setFooter(CmsUiProvider footer) {
		this.footer = footer;
	}

	public CmsUiProvider getHeader() {
		return header;
	}

	public void setFooterHeight(Integer footerHeight) {
		this.footerHeight = footerHeight;
	}

}
