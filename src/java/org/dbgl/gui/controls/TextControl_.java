package org.dbgl.gui.controls;

import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;


public class TextControl_ {

	private Text text_;
	private Combo combo_;

	public TextControl_(Text text) {
		text_ = text;
	}

	public TextControl_(Combo combo) {
		combo_ = combo;
	}

	public void setText(String text) {
		if (text_ != null)
			text_.setText(text);
		else if (combo_ != null)
			combo_.setText(text);
	}

	public String getText() {
		if (text_ != null)
			return text_.getText();
		else if (combo_ != null)
			return combo_.getText();
		return null;
	}

	public String getLineDelimiter() {
		if (text_ != null)
			return text_.getLineDelimiter();
		return null;
	}

	public void selectAll() {
		if (text_ != null)
			text_.selectAll();
	}

	public void setFocus() {
		if (text_ != null)
			text_.setFocus();
		else if (combo_ != null)
			combo_.setFocus();
	}
}
