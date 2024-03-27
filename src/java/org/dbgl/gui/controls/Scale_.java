package org.dbgl.gui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Scale;


public class Scale_ {

	private Scale_() {
	}

	public static Builder on(Composite composite) {
		return new Builder(composite);
	}

	public static class Builder extends ControlBuilder<Builder> {
		String tooltip_;
		Integer minimum_ = null;
		Integer maximum_ = null;
		Integer increment_ = null;
		Integer pageIncrement_ = null;
		Integer selection_ = null;

		Builder(Composite composite) {
			super(composite, SWT.HORIZONTAL, SWT.FILL, SWT.CENTER, true, false);
		}

		public Builder tooltip(String tooltip) {
			tooltip_ = tooltip;
			return this;
		}

		public Builder vertical() {
			style_ = SWT.VERTICAL;
			horizontalAlignment_ = SWT.CENTER;
			verticalAlignment_ = SWT.FILL;
			grabExcessHorizontalSpace_ = false;
			grabExcessVerticalSpace_ = true;
			return this;
		}

		public Builder min(int min) {
			minimum_ = min;
			return this;
		}

		public Builder max(int max) {
			maximum_ = max;
			return this;
		}

		public Builder incr(int incr) {
			increment_ = incr;
			return this;
		}

		public Builder pageIncr(int pageIncr) {
			pageIncrement_ = pageIncr;
			return this;
		}

		public Builder select(int select) {
			selection_ = select;
			return this;
		}

		public Scale build() {
			Scale scale = new Scale(composite_, style_);
			if (DarkTheme.forced()) {
				scale.setBackground(composite_.getBackground());
				scale.setForeground(DarkTheme.defaultForeground);
			}
			scale.setLayoutData(layoutData());

			if (minimum_ != null)
				scale.setMinimum(minimum_);
			if (maximum_ != null)
				scale.setMaximum(maximum_);
			if (increment_ != null)
				scale.setIncrement(increment_);
			if (pageIncrement_ != null)
				scale.setPageIncrement(pageIncrement_);
			if (tooltip_ != null)
				scale.setToolTipText(tooltip_);
			if (selection_ != null)
				scale.setSelection(selection_);
			return scale;
		}
	}
}
