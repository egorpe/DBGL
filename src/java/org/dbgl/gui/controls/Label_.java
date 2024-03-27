package org.dbgl.gui.controls;

import org.dbgl.service.TextService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;


public class Label_ {

	private Label_() {
	}

	public static Builder on(Composite composite) {
		return new Builder(composite);
	}

	public static class Builder extends ControlBuilder<Builder> {
		private String labelText_;

		Builder(Composite composite) {
			super(composite, SWT.NONE, SWT.BEGINNING, SWT.CENTER, false, false);
		}

		public Builder txt(String txt) {
			labelText_ = txt;
			return this;
		}

		public Builder key(String key) {
			return txt(TextService.getInstance().get(key));
		}

		public Builder key(String key, String param) {
			return txt(TextService.getInstance().get(key, param));
		}

		public Builder key(String key, Object[] objs) {
			return txt(TextService.getInstance().get(key, objs));
		}

		public Label build() {
			Label label = new Label(composite_, style_);
			if (DarkTheme.forced()) {
				label.setBackground(composite_.getBackground());
				label.setForeground(DarkTheme.defaultForeground);
			}
			label.setLayoutData(layoutData());
			if (labelText_ != null)
				label.setText(labelText_);
			return label;
		}
	}
}
