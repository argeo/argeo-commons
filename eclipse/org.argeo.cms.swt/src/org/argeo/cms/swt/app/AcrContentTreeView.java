package org.argeo.cms.swt.app;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.argeo.api.acr.Content;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.swt.acr.SwtContentHierarchicalPart;
import org.argeo.cms.swt.widgets.SwtTabularPart;
import org.argeo.cms.ux.widgets.Column;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public class AcrContentTreeView extends Composite {
	private static final long serialVersionUID = -3707881216246077323L;

	private Content rootContent;

	private Content selected;

	public AcrContentTreeView(Composite parent, int style, Content content) {
		super(parent, style);
		this.rootContent = content;
		this.selected = rootContent;
		setLayout(CmsSwtUtils.noSpaceGridLayout());

		SashForm split = new SashForm(this, SWT.HORIZONTAL);
		split.setLayoutData(CmsSwtUtils.fillAll());

		SwtContentHierarchicalPart hPart = new SwtContentHierarchicalPart(split, getStyle());
		Composite area = new Composite(split, SWT.NONE);
		area.setLayout(CmsSwtUtils.noSpaceGridLayout(2));
		split.setWeights(new int[] { 30, 70 });

		// attributes
		SwtTabularPart attributesPart = new SwtTabularPart(area, SWT.NONE) {
			List<QName> data;

			@Override
			protected Object getData(int row) {
				return data.get(row);
			}

			@Override
			protected int getItemCount() {
				data = new ArrayList<>(selected.keySet());
				return data.size();
			}
		};
		attributesPart.getArea().setLayoutData(CmsSwtUtils.fillHeight());

		attributesPart.addColumn(new Column<QName>() {

			@Override
			public String getText(QName model) {
				return model.toString();
			}
		});
		attributesPart.addColumn(new Column<QName>() {

			@Override
			public String getText(QName model) {
				return selected.get(model).toString();
			}

			@Override
			public int getWidth() {
				return 400;
			}

		});

		// types
		SwtTabularPart typesPart = new SwtTabularPart(area, SWT.NONE) {
			List<QName> data;

			@Override
			protected Object getData(int row) {
				return data.get(row);
			}

			@Override
			protected int getItemCount() {
				data = new ArrayList<>(selected.getContentClasses());
				return data.size();
			}
		};
		typesPart.addColumn(new Column<QName>() {

			@Override
			public String getText(QName model) {
				return model.toString();
			}
		});
		typesPart.getArea().setLayoutData(CmsSwtUtils.fillHeight());

		// controller
		hPart.setInput(rootContent);
		hPart.onSelected((o) -> {
			Content c = (Content) o;
			selected = c;
			attributesPart.refresh();
			typesPart.refresh();
		});

		attributesPart.refresh();
		typesPart.refresh();
	}

//	protected void refreshTable() {
//		for (TableItem item : table.getItems()) {
//			item.dispose();
//		}
//		for (QName key : selected.keySet()) {
//			TableItem item = new TableItem(table, 0);
//			item.setText(0, key.toString());
//			Object value = selected.get(key);
//			item.setText(1, value.toString());
//		}
//		table.getColumn(0).pack();
//		table.getColumn(1).pack();
//	}

//	public static void main(String[] args) {
//		Path basePath;
//		if (args.length > 0) {
//			basePath = Paths.get(args[0]);
//		} else {
//			basePath = Paths.get(System.getProperty("user.home"));
//		}
//
//		final Display display = new Display();
//		final Shell shell = new Shell(display);
//		shell.setText(basePath.toString());
//		shell.setLayout(new FillLayout());
//
//		FsContentProvider contentSession = new FsContentProvider("/", basePath);
////		GcrContentTreeView treeView = new GcrContentTreeView(shell, 0, contentSession.get("/"));
//
//		shell.setSize(shell.computeSize(800, 600));
//		shell.open();
//		while (!shell.isDisposed()) {
//			if (!display.readAndDispatch())
//				display.sleep();
//		}
//		display.dispose();
//	}
}
