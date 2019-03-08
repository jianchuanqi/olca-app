package org.openlca.app.results.simulation;

import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.app.viewers.combo.FlowViewer;
import org.openlca.app.viewers.combo.ImpactCategoryViewer;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.Simulator;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Unit;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.SimulationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SimulationPage extends FormPage {

	private final int FLOW = 0;
	private final int IMPACT = 1;
	private int resultType = FLOW;

	private final SimulationEditor editor;
	private final Simulator simulator;
	private final SimulationResult result;

	private StatisticsCanvas statisticsCanvas;
	private ProgressBar progressBar;
	private FlowViewer flowViewer;
	private Section progressSection;
	private ScrolledForm form;
	private ImpactCategoryViewer impactViewer;

	public SimulationPage(SimulationEditor editor) {
		super(editor, "SimulationPage", M.MonteCarloSimulation);
		this.editor = editor;
		this.simulator = editor.simulator;
		this.result = editor.simulator.getResult();
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText(M.MonteCarloSimulation);
		toolkit.decorateFormHeading(form.getForm());
		Composite body = UI.formBody(form, toolkit);
		createSettingsSection(toolkit, body);
		new PinBoard(simulator).create(toolkit, body);
		createProgressSection(toolkit, body);
		createResultSection(toolkit, body);
		form.pack();
	}

	private void createSettingsSection(FormToolkit toolkit, Composite body) {
		Composite settings = UI.formSection(body, toolkit, M.Settings);
		Text systemText = UI.formText(settings, toolkit, M.ProductSystem);
		Text processText = UI.formText(settings, toolkit, M.Process);
		Text qRefText = UI.formText(settings, toolkit, M.QuantitativeReference);
		Text simCountText = UI.formText(settings, toolkit, M.NumberOfSimulations);
		if (editor.setup != null) {
			CalculationSetup setup = editor.setup;
			systemText.setText(setup.productSystem.name);
			processText.setText(setup.productSystem.referenceProcess.name);
			qRefText.setText(getQRefText());
			simCountText.setText(Integer.toString(setup.numberOfRuns));
		}
		systemText.setEditable(false);
		processText.setEditable(false);
		qRefText.setEditable(false);
		simCountText.setEditable(false);
	}

	private String getQRefText() {
		try {
			CalculationSetup setup = editor.setup;
			ProductSystem system = setup.productSystem;
			Exchange exchange = system.referenceExchange;
			double amount = system.targetAmount;
			Flow flow = exchange.flow;
			Unit unit = system.targetUnit;
			return String.format("%s %s %s", Numbers.format(amount, 2),
					unit.name, Labels.getDisplayName(flow));
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to create text for quan. ref.", e);
			return "";
		}
	}

	private void createProgressSection(FormToolkit toolkit, Composite body) {
		progressSection = UI.section(body, toolkit, M.Progress);
		Composite composite = UI.sectionClient(progressSection, toolkit);
		progressBar = new ProgressBar(composite, SWT.SMOOTH);
		progressBar.setMaximum(editor.setup.numberOfRuns);
		UI.gridData(progressBar, false, false).widthHint = 470;
		Button progressButton = toolkit.createButton(composite,
				M.Start, SWT.NONE);
		UI.gridData(progressButton, false, false).widthHint = 70;
		new SimulationControl(progressButton, editor, this);
	}

	private void createResultSection(FormToolkit toolkit, Composite body) {
		if (result == null)
			return;
		Section section = UI.section(body, toolkit, M.Results);
		SimulationExportAction exportAction = new SimulationExportAction(
				result, editor.setup);
		Actions.bind(section, exportAction);
		Composite composite = UI.sectionClient(section, toolkit);
		initFlowCheckViewer(toolkit, composite);
		if (result.hasImpactResults())
			initImpactCheckViewer(toolkit, composite);
		statisticsCanvas = new StatisticsCanvas(body);
		UI.gridData(statisticsCanvas, true, true).verticalIndent = 5;
	}

	private void initImpactCheckViewer(FormToolkit toolkit, Composite section) {
		Button impactCheck = toolkit.createButton(section,
				M.ImpactCategories, SWT.RADIO);
		impactViewer = new ImpactCategoryViewer(section);
		impactViewer.setEnabled(false);
		Set<ImpactCategoryDescriptor> impacts = result.getImpacts();
		impactViewer.setInput(impacts);
		impactViewer.addSelectionChangedListener((e) -> updateSelection());
		impactViewer.selectFirst();
		new ResultTypeCheck<>(impactViewer, impactCheck, IMPACT);
	}

	private void initFlowCheckViewer(FormToolkit toolkit, Composite section) {
		Button flowsCheck = toolkit.createButton(section, M.Flows,
				SWT.RADIO);
		flowsCheck.setSelection(true);
		flowViewer = new FlowViewer(section);
		Set<FlowDescriptor> flows = result.getFlows();
		flowViewer.setInput(flows.toArray(new FlowDescriptor[flows.size()]));
		flowViewer.selectFirst();
		flowViewer.addSelectionChangedListener((e) -> updateSelection());
		new ResultTypeCheck<>(flowViewer, flowsCheck, FLOW);
	}

	private void updateSelection() {
		if (result == null || statisticsCanvas == null)
			return;
		if (resultType == FLOW) {
			FlowDescriptor flow = flowViewer.getSelected();
			if (flow != null)
				statisticsCanvas.setValues(result.getFlowResults(flow));
		} else {
			ImpactCategoryDescriptor cat = impactViewer.getSelected();
			if (cat != null)
				statisticsCanvas.setValues(result.getImpactResults(cat));
		}
	}

	void updateProgress() {
		if (result == null)
			return;
		updateSelection();
		progressBar.setSelection(progressBar.getSelection() + 1);
	}

	void progressDone(int numberOfIteration) {
		progressSection.dispose();
		form.reflow(true);
	}

	private class ResultTypeCheck<T> implements SelectionListener {

		private AbstractComboViewer<T> viewer;
		private Button check;
		private int type;

		public ResultTypeCheck(AbstractComboViewer<T> viewer, Button check,
				int type) {
			this.viewer = viewer;
			this.check = check;
			this.type = type;
			check.addSelectionListener(this);
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			if (check.getSelection()) {
				viewer.setEnabled(true);
				resultType = this.type;
				updateSelection();
			} else
				viewer.setEnabled(false);
		}
	}

}
