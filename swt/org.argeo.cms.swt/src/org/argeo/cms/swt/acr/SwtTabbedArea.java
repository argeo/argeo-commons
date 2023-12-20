package org.argeo.cms.swt.acr;

import java.util.ArrayList;
import java.util.List;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.spi.ProvidedContent;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.swt.Selected;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/** Manages {@link SwtSection} in a tab-like structure. */
public class SwtTabbedArea extends Composite {
	private static final long serialVersionUID = 8659669229482033444L;

	private Composite headers;
	private Composite body;

	private List<SwtSection> sections = new ArrayList<>();

	private ProvidedContent previousNode;
	private SwtUiProvider previousUiProvider;
	private SwtUiProvider currentUiProvider;

	private String tabStyle;
	private String tabSelectedStyle;
	private String bodyStyle;
	private Image closeIcon;

	private StackLayout stackLayout;

	private boolean singleTab = false;
	private String singleTabTitle = null;

	public SwtTabbedArea(Composite parent, int style) {
		super(parent, SWT.NONE);
		CmsSwtUtils.style(parent, bodyStyle);

		setLayout(CmsSwtUtils.noSpaceGridLayout());

		// TODO manage tabs at bottom or sides
		headers = new Composite(this, SWT.NONE);
		headers.setLayoutData(CmsSwtUtils.fillWidth());
		body = new Composite(this, SWT.NONE);
		body.setLayoutData(CmsSwtUtils.fillAll());
		// body.setLayout(new FormLayout());
		stackLayout = new StackLayout();
		body.setLayout(stackLayout);
		emptyState();
	}

	protected void refreshTabHeaders() {
		int tabCount = sections.size() > 0 ? sections.size() : 1;
		for (Control tab : headers.getChildren())
			tab.dispose();

		headers.setLayout(CmsSwtUtils.noSpaceGridLayout(new GridLayout(tabCount, true)));

		if (sections.size() == 0) {
			Composite emptyHeader = new Composite(headers, SWT.NONE);
			emptyHeader.setLayoutData(CmsSwtUtils.fillAll());
			emptyHeader.setLayout(new GridLayout());
			Label lbl = new Label(emptyHeader, SWT.NONE);
			lbl.setText("");
			lbl.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));

		}

		SwtSection currentSection = getCurrentSection();
		for (SwtSection section : sections) {
			boolean selected = section == currentSection;
			Composite sectionHeader = section.createHeader(headers);
			CmsSwtUtils.style(sectionHeader, selected ? tabSelectedStyle : tabStyle);
			int headerColumns = singleTab ? 1 : 2;
			sectionHeader.setLayout(new GridLayout(headerColumns, false));
			sectionHeader.setLayout(CmsSwtUtils.noSpaceGridLayout(headerColumns));
			Button title = new Button(sectionHeader, SWT.FLAT);
			CmsSwtUtils.style(title, selected ? tabSelectedStyle : tabStyle);
			title.setLayoutData(CmsSwtUtils.fillWidth());
			title.addSelectionListener((Selected) (e) -> showTab(tabIndex(section.getContent())));
			Content node = section.getContent();

			// FIXME find a standard way to display titles
			String titleStr = node.getName().getLocalPart();
			if (singleTab && singleTabTitle != null)
				titleStr = singleTabTitle;

			// TODO internationalise
			title.setText(titleStr);
			if (!singleTab) {
				ToolBar toolBar = new ToolBar(sectionHeader, SWT.NONE);
				ToolItem closeItem = new ToolItem(toolBar, SWT.FLAT);
				if (closeIcon != null)
					closeItem.setImage(closeIcon);
				else
					closeItem.setText("X");
				CmsSwtUtils.style(closeItem, selected ? tabSelectedStyle : tabStyle);
				closeItem.addSelectionListener((Selected) (e) -> closeTab(section));
			}
		}

	}

	public void view(SwtUiProvider uiProvider, Content context) {
		if (body.isDisposed())
			return;
		int index = tabIndex(context);
		if (index >= 0) {
			showTab(index);
			previousNode = (ProvidedContent) context;
			previousUiProvider = uiProvider;
			return;
		}
		SwtSection section = (SwtSection) body.getChildren()[0];
		previousNode = (ProvidedContent) section.getContent();
		if (previousNode == null) {// empty state
			previousNode = (ProvidedContent) context;
			previousUiProvider = uiProvider;
		} else {
			previousUiProvider = currentUiProvider;
		}
		currentUiProvider = uiProvider;
		section.setContent(context);
		// section.setLayoutData(CmsUiUtils.coverAll());
		build(section, uiProvider, context);
		if (sections.size() == 0)
			sections.add(section);
		refreshTabHeaders();
		index = tabIndex(context);
		showTab(index);
		layout(true, true);
	}

	public void open(SwtUiProvider uiProvider, Content context) {
		if (singleTab)
			throw new UnsupportedOperationException("Open is not supported in single tab mode.");

		if (previousNode != null
				&& previousNode.getSessionLocalId().equals(((ProvidedContent) context).getSessionLocalId())) {
			// does nothing
			return;
		}
		if (sections.size() == 0)
			CmsSwtUtils.clear(body);
		SwtSection currentSection = getCurrentSection();
		int currentIndex = sections.indexOf(currentSection);
		SwtSection previousSection = new SwtSection(body, SWT.NONE, context);
		build(previousSection, previousUiProvider, previousNode);
		// previousSection.setLayoutData(CmsUiUtils.coverAll());
		int newIndex = currentIndex + 1;
		sections.add(currentIndex, previousSection);
//		sections.add(newIndex, previousSection);
		showTab(newIndex);
		refreshTabHeaders();
		layout(true, true);
	}

	public void showTab(int index) {
		SwtSection sectionToShow = sections.get(index);
		// sectionToShow.moveAbove(null);
		stackLayout.topControl = sectionToShow;
		refreshTabHeaders();
		layout(true, true);
	}

	protected void build(SwtSection section, SwtUiProvider uiProvider, Content context) {
		for (Control child : section.getChildren())
			child.dispose();
		CmsSwtUtils.style(section, bodyStyle);
		section.setContent(context);
		uiProvider.createUiPart(section, context);

	}

	private int tabIndex(Content context) {
		for (int i = 0; i < sections.size(); i++) {
			SwtSection section = sections.get(i);
			if (section.getSessionLocalId().equals(((ProvidedContent) context).getSessionLocalId()))
				return i;
		}
		return -1;
	}

	public void closeTab(SwtSection section) {
		int currentIndex = sections.indexOf(section);
		int nextIndex = currentIndex == 0 ? 0 : currentIndex - 1;
		sections.remove(section);
		section.dispose();
		if (sections.size() == 0) {
			emptyState();
			refreshTabHeaders();
			layout(true, true);
			return;
		}
		refreshTabHeaders();
		showTab(nextIndex);
	}

	public void closeAllTabs() {
		for (SwtSection section : sections) {
			section.dispose();
		}
		sections.clear();
		emptyState();
		refreshTabHeaders();
		layout(true, true);
	}

	protected void emptyState() {
		new SwtSection(body, SWT.NONE, null);
		refreshTabHeaders();
	}

	public Composite getCurrent() {
		return getCurrentSection();
	}

	protected SwtSection getCurrentSection() {
		return (SwtSection) stackLayout.topControl;
	}

	public Content getCurrentContext() {
		SwtSection section = getCurrentSection();
		if (section != null && !section.isDisposed()) {
			return section.getContent();
		} else {
			return null;
		}
	}

	public void setTabStyle(String tabStyle) {
		this.tabStyle = tabStyle;
	}

	public void setTabSelectedStyle(String tabSelectedStyle) {
		this.tabSelectedStyle = tabSelectedStyle;
	}

	public void setBodyStyle(String bodyStyle) {
		this.bodyStyle = bodyStyle;
	}

	public void setCloseIcon(Image closeIcon) {
		this.closeIcon = closeIcon;
	}

	public void setSingleTab(boolean singleTab) {
		this.singleTab = singleTab;
	}

	public void setSingleTabTitle(String singleTabTitle) {
		this.singleTabTitle = singleTabTitle;
	}

}
