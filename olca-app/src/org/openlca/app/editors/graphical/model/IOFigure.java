package org.openlca.app.editors.graphical.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graphical.themes.Theme.Box;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;

class IOFigure extends Figure {

	private final ProcessNode node;
	private final ExchangePanel inputPanel;
	private final ExchangePanel outputPanel;

	IOFigure(ProcessNode node) {
		this.node = node;
		var layout = new GridLayout(1, true);
		layout.horizontalSpacing = 4;
		layout.verticalSpacing = 4;
		layout.marginHeight = 5;
		layout.marginWidth = 0;
		setLayoutManager(layout);
		inputPanel = initPanel(true);
		outputPanel = initPanel(false);
	}

	private ExchangePanel initPanel(boolean forInputs) {
		add(new Header(forInputs), new GridData(SWT.FILL, SWT.TOP, true, false));
		var panel = new ExchangePanel(node);
		add(panel, new GridData(SWT.FILL, SWT.FILL, true, true));
		var figure = new ExchangeFigure(node.getExchangeNodes().get(0));
		add(new FlowButton(figure, forInputs, node), new GridData(SWT.FILL, SWT.TOP, true, false));
		return panel;
	}

	@Override
	public void add(IFigure figure, Object constraint, int index) {

		if (!(figure instanceof ExchangeFigure)) {
			super.add(figure, constraint, index);
			return;
		}

		// delegate exchange figures to the respective input or
		// output panel
		var ef = (ExchangeFigure) figure;
		if (ef.node == null || ef.node.exchange == null)
			return;
		var exchange = ef.node.exchange;
		var panel = exchange.isInput ? inputPanel : outputPanel;
		ExchangeRow.create(ef, panel);
	}

	private class Header extends Figure {

		private final Label label;

		Header(boolean forInputs) {
			var layout = new GridLayout(1, true);
			layout.marginHeight = 3;
			layout.marginWidth = 5;
			setLayoutManager(layout);
			label = new Label(forInputs ? ">> input flows" : "output flows >>");
			var alignment = forInputs ? SWT.LEFT : SWT.RIGHT;
			add(label, new GridData(alignment, SWT.TOP, true, false));
		}

		@Override
		public void paint(Graphics g) {
			var theme = node.config().theme();
			var location = getLocation();
			var size = getSize();
			g.setForegroundColor(theme.boxBorderColor(Box.of(node)));
			g.drawLine(location.x, location.y, location.x + size.width, location.y);
			g.restoreState();
			label.setForegroundColor(theme.infoLabelColor());
			super.paint(g);
		}
	}

	private static class ExchangePanel extends Figure {

		private final ProcessNode node;
		private final List<ExchangeRow> rows = new ArrayList<>();

		ExchangePanel(ProcessNode node) {
			this.node = node;
			var config = node.config();
			int columns = config.showFlowIcons ? 2 : 1;
			if (config.showFlowAmounts) {
				columns += 2;
			}
			var layout = new GridLayout(columns, false);
			layout.marginHeight = 4;
			layout.marginWidth = 5;
			setLayoutManager(layout);
		}

		@Override
		protected void paintFigure(Graphics g) {
			// set a specific background if this is required
			rows.forEach(ExchangeRow::updateStyle);
			var theme = node.config().theme();
			g.pushState();
			var background = theme.boxBackgroundColor(Box.of(node));
			g.setBackgroundColor(background);
			var loc = getLocation();
			var size = getSize();
			g.fillRectangle(loc.x, loc.y, size.width, size.height);
			g.popState();
			super.paintFigure(g);
		}
	}

	private static class ExchangeRow {

		private final ImageFigure icon;
		private final ExchangeFigure figure;
		private final Label amountLabel;
		private final Label unitLabel;

		private ExchangeRow(ImageFigure icon, ExchangeFigure figure, Label amountLabel, Label unitLabel) {
			this.icon = icon;
			this.figure = figure;
			this.amountLabel = amountLabel;
			this.unitLabel = unitLabel;
		}

		static void create(ExchangeFigure figure, ExchangePanel panel) {
			if (figure == null || panel == null || figure.node == null || figure.node.exchange == null)
				return;

			var config = figure.node.config();
			var flowType = figure.node.flowType();
			var exchange = figure.node.exchange;

			var icon = config.showFlowIcons ? add(panel, SWT.LEFT, new ImageFigure(Images.get(flowType))) : null;
			add(panel, SWT.FILL, figure);
			var amount = config.showFlowAmounts ? add(panel, SWT.RIGHT, new Label(Numbers.format(exchange.amount, 2)))
					: null;
			var unit = config.showFlowAmounts ? add(panel, SWT.LEFT, new Label(Labels.name(exchange.unit))) : null;

			var row = new ExchangeRow(icon, figure, amount, unit);
			panel.rows.add(row);
		}

		private static <T extends Figure> T add(ExchangePanel panel, int hAlign, T figure) {
			panel.add(figure, new GridData(hAlign, SWT.TOP, hAlign == SWT.FILL, false));
			return figure;
		}

		void updateStyle() {
			var theme = figure.node.config().theme();
			var flowType = figure.node.flowType();
			if (amountLabel != null) {
				var color = theme.labelColor(flowType);
				amountLabel.setForegroundColor(color);
				unitLabel.setForegroundColor(color);
			}
		}

	}

}
