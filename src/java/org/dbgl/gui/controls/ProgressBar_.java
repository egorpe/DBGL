package org.dbgl.gui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ProgressBar;


public class ProgressBar_ {

	private ProgressBar_() {
	}

	public static Builder on(Composite composite) {
		return new Builder(composite);
	}

	public static final class Builder extends ControlBuilder<Builder> {
		private int sel_ = Integer.MIN_VALUE;
		private int max_ = 100;

		Builder(Composite composite) {
			super(composite, SWT.HORIZONTAL, SWT.FILL, SWT.CENTER, true, false);
		}

		public Builder indeterminate() {
			style_ |= SWT.INDETERMINATE;
			return this;
		}

		public Builder sel(int sel) {
			sel_ = sel;
			return this;
		}

		public Builder max(int max) {
			max_ = max;
			return this;
		}

		public ProgressBar build() {
			ProgressBar bar = new ProgressBar(composite_, style_);
			if (DarkTheme.forced()) {
				bar.setBackground(DarkTheme.inputBackground);
			}
			bar.setLayoutData(layoutData());
			if ((style_ & SWT.INDETERMINATE) != SWT.INDETERMINATE)
				bar.setMaximum(max_);
			if (sel_ != Integer.MIN_VALUE)
				bar.setSelection(sel_);
			return bar;
		}
	}
}
