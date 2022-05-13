package org.openlca.app.editors.graph.figures;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.core.model.descriptors.RootDescriptor;

public class MaximizedNodeFigure extends Figure {

	private final RootDescriptor descriptor;
	private final Figure contentPane = new Figure();

	private LayoutManager layout;

	public MaximizedNodeFigure(RootDescriptor descriptor) {
		this.descriptor = descriptor;
		var name = Labels.name(descriptor);

		layout = new GridLayout(1, false);
		setLayoutManager(layout);

		var roundedCorners = RoundBorder.Corners
			.fullRoundedCorners(new Dimension(15, 15));
		var border = new RoundBorder(2, roundedCorners);
		border.setColor(ColorConstants.black);
		setBorder(border);

		setToolTip(new Label(name));
		setForegroundColor(Colors.white());

		var header = new NodeHeader(name, Images.get(this.descriptor));
		var topRoundedCorners = RoundBorder.Corners
			.topRoundedCorners(new Dimension(15, 15));
		var headerBorder = new RoundBorder(2, topRoundedCorners);
		headerBorder.setColor(ColorConstants.black);
		header.setBorder(headerBorder);
		add(header, new GridData(SWT.FILL, SWT.FILL, true, false));

		contentPane.setLayoutManager(new GridLayout(1, false));
		add(contentPane);

		setOpaque(true);
	}

	public IFigure getContentPane() {
		return contentPane;
	}

}
