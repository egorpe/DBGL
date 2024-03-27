package org.dbgl.gui.controls;

import org.dbgl.service.TextService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;


public class Spinner_ {

	private Spinner_() {
	}

	public static Builder on(Composite composite) {
		return new Builder(composite);
	}

	public static class Builder extends ControlBuilder<Builder> {
		String tooltip_;
		Integer minimum_ = null;
		Integer maximum_ = null;
		Integer digits_ = null;
		Integer increment_ = null;
		Integer pageIncrement_ = null;
		Integer selection_ = null;

		Builder(Composite composite) {
			super(composite, SWT.BORDER, SWT.FILL, SWT.CENTER, false, false);
		}

		public Builder tooltip(String key) {
			tooltip_ = TextService.getInstance().get(key);
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

		public Builder digits(int digits) {
			digits_ = digits;
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

		public Spinner build() {
			Spinner spinner = new Spinner(composite_, style_);
			if (DarkTheme.forced()) {
				spinner.setBackground(DarkTheme.inputBackground);
				spinner.setForeground(DarkTheme.defaultForeground);
			}
			spinner.setLayoutData(layoutData());
			if (minimum_ != null)
				spinner.setMinimum(minimum_);
			if (maximum_ != null)
				spinner.setMaximum(maximum_);
			if (digits_ != null)
				spinner.setDigits(digits_);
			if (increment_ != null)
				spinner.setIncrement(increment_);
			if (pageIncrement_ != null)
				spinner.setPageIncrement(pageIncrement_);
			if (tooltip_ != null)
				spinner.setToolTipText(tooltip_);
			if (selection_ != null)
				spinner.setSelection(selection_);
			return spinner;
		}
	}
}
