package org.openlca.app.editors.graphical.action;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphConfig;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.ColorfulTheme;
import org.openlca.app.editors.graphical.model.WhiteTheme;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.util.Pair;

public class GraphSettingsAction extends Action {

	private final GraphEditor editor;

	public GraphSettingsAction(GraphEditor editor) {
		this.editor = editor;
		setId("GraphSettingsAction");
		setText(M.Settings);
		setImageDescriptor(Icon.PREFERENCES.descriptor());
	}

	@Override
	public void run() {
		if (editor == null)
			return;
		var config = GraphConfig.from(editor.config);
		if (new Dialog(config).open() == Window.OK) {
			config.applyOn(editor.config);
			editor.refresh();
			editor.setDirty();
			editor.getModel().figure.repaint();
		}
	}

	private static class Dialog extends FormDialog {

		private final GraphConfig config;

		Dialog(GraphConfig config) {
			super(UI.shell());
			setBlockOnOpen(true);
			this.config = config;
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var tk = mform.getToolkit();
			var body = UI.formBody(mform.getForm(), tk);
			UI.gridLayout(body, 1);

			var icons = tk.createButton(body, "Show flow icons", SWT.CHECK);
			icons.setSelection(config.showFlowIcons);
			Controls.onSelect(icons,
					e -> config.showFlowIcons = icons.getSelection());

			var elems = tk.createButton(body, "Show elementary flows", SWT.CHECK);
			elems.setSelection(config.showElementaryFlows);
			Controls.onSelect(elems,
					e -> config.showElementaryFlows = elems.getSelection());

			var amounts = tk.createButton(body, "Show flow amounts", SWT.CHECK);
			amounts.setSelection(config.showFlowAmounts);
			Controls.onSelect(amounts,
					e -> config.showFlowAmounts = amounts.getSelection());

			// theme combo
			var comp = tk.createComposite(body);
			UI.gridLayout(comp, 2, 10, 0);
			var themeCombo = UI.formCombo(comp, tk, "Theme");
			var themes = List.of(
				Pair.of("Colorful", new ColorfulTheme()),
				Pair.of("White", new WhiteTheme())
			);
			int idx = 0;
			for (int i = 0; i < themes.size(); i++) {
				var pair = themes.get(i);
				themeCombo.add(pair.first);
				if (pair.second.getClass().equals(config.theme().getClass())) {
					idx = i;
				}
			}
			themeCombo.select(idx);
			Controls.onSelect(themeCombo, _e -> {
				var pair = themes.get(themeCombo.getSelectionIndex());
				config.theme(pair.second);
			});
		}

		@Override
		protected Point getInitialSize() {
			var shell = getShell().getDisplay().getBounds();
			int width = shell.x > 0 && shell.x < 600
					? shell.x
					: 600;
			int height = shell.y > 0 && shell.y < 350
					? shell.y
					: 350;
			return new Point(width, height);
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText(M.Settings);
		}
	}
}
