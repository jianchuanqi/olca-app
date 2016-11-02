package org.openlca.app.results;

import org.openlca.app.M;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.util.trees.Trees;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.DQIndicator;
import org.openlca.core.model.DQSystem;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.SimpleResultProvider;

public class DQInfoSection {

	private final SimpleResultProvider<?> result;
	private final DQResult dqResult;
	private final FormToolkit toolkit;

	public DQInfoSection(Composite parent, FormToolkit toolkit, SimpleResultProvider<?> result, DQResult dqResult) {
		this.result = result;
		this.dqResult = dqResult;
		this.toolkit = toolkit;
		create(parent);
	}

	private void create(Composite parent) {
		Composite client = UI.formSection(parent, toolkit, M.DataQuality);
		createText(client, M.ProcessDataQualityScheme, dqResult.setup.processDqSystem);
		createText(client, M.InputOutputDataQualityScheme, dqResult.setup.exchangeDqSystem);
		createText(client, M.Aggregation, Labels.aggregationType(dqResult.setup.aggregationType));
		createText(client, M.RoundingMode, Labels.roundingMode(dqResult.setup.roundingMode));
		createText(client, M.NaValueHandling, Labels.processingType(dqResult.setup.processingType));
		createStatisticsTree(client, M.ProcessDataQualityStatistics, true);
		createStatisticsTree(client, M.InputOutputDataQualityStatistics, false);
	}

	private void createText(Composite parent, String label, DQSystem system) {
		if (system == null)
			createText(parent, label, "-- No data quality system specified --");
		else
			createText(parent, label, system.getName());
	}

	private void createText(Composite parent, String label, String value) {
		Text text = UI.formText(parent, label);
		text.setText(value);
		text.setEditable(false);
	}

	private void createStatisticsTree(Composite parent, String label, boolean forProcesses) {
		DQSystem system = forProcesses ? dqResult.setup.processDqSystem : dqResult.setup.exchangeDqSystem;
		if (system == null)
			return;
		UI.formLabel(parent, toolkit, label);
		UI.formLabel(parent, toolkit, "");
		String[] headers = { "Indicator", "Coverage" };
		TreeViewer viewer = Trees.createViewer(parent, headers);
		viewer.setContentProvider(new ContentProvider(forProcesses));
		viewer.setLabelProvider(new LabelProvider(forProcesses));
		UI.gridData(viewer.getTree(), true, true).horizontalSpan = 2;
		viewer.setInput(system.indicators);
	}

	private class ContentProvider extends ArrayContentProvider implements ITreeContentProvider {

		private final boolean forProcesses;

		private ContentProvider(boolean forProcesses) {
			this.forProcesses = forProcesses;
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (!(parentElement instanceof DQIndicator) && !forProcesses)
				return null;
			Set<ProcessDescriptor> processes = result.getProcessDescriptors();
			List<Object> children = new ArrayList<>();
			for (ProcessDescriptor process : processes) {
				children.add(new Tupel(process, (DQIndicator) parentElement));
			}
			return children.toArray();
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return element instanceof DQIndicator && !forProcesses;
		}

	}

	private class LabelProvider extends BaseLabelProvider implements ITableLabelProvider {

		private final boolean forProcesses;

		private LabelProvider(boolean forProcesses) {
			this.forProcesses = forProcesses;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex != 0)
				return null;
			if (element instanceof DQIndicator)
				return Images.get(ModelType.DQ_SYSTEM);
			return Images.get(ModelType.PROCESS);
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof DQIndicator)
				return getText((DQIndicator) element, columnIndex);
			return getText((Tupel) element, columnIndex);
		}

		private String getText(DQIndicator indicator, int col) {
			switch (col) {
			case 0:
				return indicator.name;
			case 1:
				int withQuality = 0;
				int total = 0;
				if (forProcesses) {
					withQuality = dqResult.statistics.getNoOfProcesses(indicator);
					total = dqResult.statistics.getNoOfProcesses();
				} else {
					withQuality = dqResult.statistics.getNoOfExchanges(indicator);
					total = dqResult.statistics.getNoOfExchanges();
				}
				return getCoverage(withQuality, total);
			default:
				return null;
			}
		}

		private String getText(Tupel value, int col) {
			switch (col) {
			case 0:
				return value.process.getName();
			case 1:
				int withQuality = dqResult.statistics.getNoOfExchanges(value.process, value.indicator);
				int total = dqResult.statistics.getNoOfExchanges(value.process);
				return getCoverage(withQuality, total);
			default:
				return null;
			}
		}

		private String getCoverage(int withQuality, int total) {
			if (total == 0d)
				return "0% (0/0)";
			double coverage = Math.round(10000d * withQuality / (double) total) / 100d;
			return coverage + "% (" + withQuality + "/" + total + ")";
		}
	}

	private class Tupel {

		private final ProcessDescriptor process;
		private final DQIndicator indicator;

		private Tupel(ProcessDescriptor process, DQIndicator indicator) {
			this.process = process;
			this.indicator = indicator;
		}

	}

}
