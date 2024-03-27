package org.dbgl.gui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;


public class Table_ {

	private Table_() {
	}

	public static Builder on(Composite composite) {
		return new Builder(composite);
	}

	public static final class Builder extends ControlBuilder<Builder> {
		private boolean header_;

		Builder(Composite composite) {
			super(composite, SWT.BORDER | SWT.FULL_SELECTION, SWT.FILL, SWT.CENTER, true, false);
		}

		public Builder header() {
			header_ = true;
			return this;
		}

		public Builder multi() {
			style_ |= SWT.MULTI;
			return this;
		}

		public Builder check() {
			style_ |= SWT.CHECK;
			return this;
		}

		public Builder scroll() {
			style_ |= SWT.H_SCROLL;
			style_ |= SWT.V_SCROLL;
			verticalAlignment_ = SWT.FILL;
			grabExcessVerticalSpace_ = true;
			return this;
		}

		public Table build() {
			Table table = new Table(composite_, style_);
			table.setHeaderVisible(header_);
			if (DarkTheme.forced()) {
				table.setBackground(DarkTheme.inputBackground);
				table.setForeground(DarkTheme.defaultForeground);
				table.setHeaderBackground(DarkTheme.tableHeaderBackground);
				table.setHeaderForeground(DarkTheme.tableHeaderForeground);
			}
			table.setLayoutData(layoutData());
			table.setLinesVisible(!DarkTheme.forced());
			return table;
		}
	}
}
