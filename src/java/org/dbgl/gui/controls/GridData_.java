package org.dbgl.gui.controls;

import org.eclipse.swt.layout.GridData;


public class GridData_ {

	private final GridData gridData_;

	public GridData_() {
		gridData_ = new GridData();
	}

	public GridData_(int widthHint) {
		this();
		widthHint(widthHint);
	}

	public GridData_(int horizontalAlignment, int verticalAlignment, boolean grabExcessHorizontalSpace, boolean grabExcessVerticalSpace) {
		gridData_ = new GridData(horizontalAlignment, verticalAlignment, grabExcessHorizontalSpace, grabExcessVerticalSpace);
	}

	public GridData_(int horizontalAlignment, int verticalAlignment, boolean grabExcessHorizontalSpace, boolean grabExcessVerticalSpace, int horizontalSpan, int verticalSpan) {
		gridData_ = new GridData(horizontalAlignment, verticalAlignment, grabExcessHorizontalSpace, grabExcessVerticalSpace, horizontalSpan, verticalSpan);
	}

	public GridData_ widthHint(int widthHint) {
		gridData_.widthHint = widthHint;
		return this;
	}

	public GridData_ heightHint(int heightHint) {
		gridData_.heightHint = heightHint;
		return this;
	}

	public GridData_ horizontalIndent(int horizontalIndent) {
		gridData_.horizontalIndent = horizontalIndent;
		return this;
	}

	public GridData_ verticalIndent(int verticalIndent) {
		gridData_.verticalIndent = verticalIndent;
		return this;
	}

	public GridData build() {
		return gridData_;
	}
}
