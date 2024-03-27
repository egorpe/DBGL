package org.dbgl.gui.controls;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.service.TextService;
import org.dbgl.util.StringRelatedUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;


public class Mess_ {

	private Mess_() {
	}

	public static Builder on(Shell shell) {
		return new Builder(shell);
	}

	public static final class Builder {
		private Shell shell_;
		private int style_ = SWT.APPLICATION_MODAL | SWT.ICON_INFORMATION | SWT.OK;
		private PrintStream printStream_ = System.out;
		private String title_ = TextService.getInstance().get("general.information");
		private List<String> messages_ = new ArrayList<>();
		private Exception exception_;
		private Control control_;
		private CTabItem tabItem_;

		public Builder(Shell shell) {
			shell_ = shell;
		}

		public Builder txt(String txt) {
			messages_.add(txt);
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

		public Builder exception(Exception e) {
			exception_ = e;
			return this;
		}

		public Builder bind(Control control) {
			if (control_ == null)
				control_ = control;
			return this;
		}

		public Builder bind(Control control, CTabItem tabItem) {
			if (control_ == null) {
				control_ = control;
				tabItem_ = tabItem;
			}
			return this;
		}

		public int display() {
			String msg = StringRelatedUtils.stringArrayToString(messages_.toArray(new String[messages_.size()]), StringUtils.LF);
			if (StringUtils.isBlank(msg) && exception_ != null)
				msg += StringRelatedUtils.toString(exception_);

			printStream_.println(title_ + ": " + msg);
			if (exception_ != null)
				exception_.printStackTrace(printStream_);

			if (shell_ != null) {
				MessageBox messageBox = new MessageBox(shell_, style_);
				messageBox.setText(title_);
				messageBox.setMessage(msg);
				return messageBox.open();
			}
			return SWT.NO;
		}

		public void warning() {
			style_ = SWT.APPLICATION_MODAL | SWT.ICON_WARNING | SWT.OK;
			title_ = TextService.getInstance().get("general.warning");
			printStream_ = System.err;
			display();
		}

		public void fatal() {
			style_ = SWT.APPLICATION_MODAL | SWT.ICON_ERROR | SWT.OK;
			title_ = TextService.getInstance().get("general.fatalerror");
			printStream_ = System.err;
			display();
		}

		public boolean confirm() {
			style_ = SWT.APPLICATION_MODAL | SWT.ICON_WARNING | SWT.YES | SWT.NO;
			title_ = TextService.getInstance().get("general.confirmation");
			return display() == SWT.YES;
		}

		public boolean noErrors() {
			return messages_.isEmpty();
		}

		public boolean valid() {
			if (noErrors())
				return true;

			display();
			if (tabItem_ != null)
				tabItem_.getParent().setSelection(tabItem_);
			control_.setFocus();
			return false;
		}
	}
}
