package org.dbgl.gui.controls;

import org.dbgl.service.TextService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Layout;


public class Group_ {

	private Group_() {
	}

	public static Builder on(Composite composite) {
		return new Builder(composite);
	}

	public static class Builder extends ControlBuilder<Builder> {
		private String text_;
		private Layout layout_;

		Builder(Composite composite) {
			super(composite, SWT.NONE, SWT.BEGINNING, SWT.TOP, false, false);
		}

		public Builder layout(Layout layout) {
			layout_ = layout;
			return this;
		}

		public Builder txt(String text) {
			text_ = text;
			return this;
		}

		public Builder key(String key) {
			return txt(TextService.getInstance().get(key));
		}

		public Group build() {
			Group group = new Group(composite_, style_);
			if (DarkTheme.forced()) {
				group.setBackground(composite_.getBackground());
				group.setForeground(DarkTheme.defaultForeground);
			}
			group.setLayoutData(layoutData());
			group.setLayout(layout_);
			if (text_ != null)
				group.setText(text_);
			return group;
		}
	}
}
