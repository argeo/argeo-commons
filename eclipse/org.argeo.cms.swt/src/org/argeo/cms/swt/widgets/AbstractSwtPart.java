package org.argeo.cms.swt.widgets;

import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.ux.widgets.DataPart;
import org.argeo.cms.ux.widgets.DataView;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;

public abstract class AbstractSwtPart<INPUT, TYPE> extends Composite implements DataView<INPUT, TYPE> {
	private static final long serialVersionUID = -1999179054267812170L;

	protected DataPart<INPUT, TYPE> dataPart;

	protected final SelectionListener selectionListener;

	public AbstractSwtPart(Composite parent, int style, DataPart<INPUT, TYPE> dataPart) {
		super(parent, style);
		setLayout(CmsSwtUtils.noSpaceGridLayout());

		this.dataPart = dataPart;

		selectionListener = new SelectionListener() {

			private static final long serialVersionUID = 4334785560035009330L;

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (dataPart.getOnSelected() != null)
					dataPart.getOnSelected().accept((TYPE) e.item.getData());
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				if (dataPart.getOnAction() != null)
					dataPart.getOnAction().accept((TYPE) e.item.getData());
			}
		};

		dataPart.addView(this);
		addDisposeListener((e) -> dataPart.removeView(this));
	}

	public abstract void refresh();
}
