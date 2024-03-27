package org.dbgl.gui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;


public class List_ {

	private List_() {
	}

	public static Builder on(Composite composite) {
		return new Builder(composite);
	}

	public static class Builder extends ControlBuilder<Builder> {
		String[] items_;
		int select_ = -1;

		Builder(Composite composite) {
			super(composite, SWT.BORDER | SWT.V_SCROLL, SWT.FILL, SWT.FILL, true, true);
		}

		public Builder multi() {
			style_ |= SWT.MULTI;
			return this;
		}

		public Builder items(String[] items) {
			items_ = items;
			return this;
		}

		public Builder select(int select) {
			select_ = select;
			return this;
		}

		public org.eclipse.swt.widgets.List build() {
			org.eclipse.swt.widgets.List list = new org.eclipse.swt.widgets.List(composite_, style_);
			if (DarkTheme.forced()) {
				list.setBackground(DarkTheme.inputBackground);
				list.setForeground(DarkTheme.defaultForeground);
			}
			list.setLayoutData(layoutData());

			if (items_ != null)
				list.setItems(items_);
			if (select_ >= 0) {
				list.select(select_);
				list.showSelection();
			}
			return list;
		}
	}
}
