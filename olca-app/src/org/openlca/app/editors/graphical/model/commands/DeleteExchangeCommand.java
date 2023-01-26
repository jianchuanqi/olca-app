package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.*;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.core.database.usage.ExchangeUseSearch;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.openlca.app.editors.processes.exchanges.Exchanges.*;

public class DeleteExchangeCommand extends Command {

	/**
	 * ExchangeItem to remove.
	 */
	private final ExchangeItem child;
	/**
	 * IOPane to remove from.
	 */
	private final IOPane parent;
	/**
	 * Node to remove from.
	 */
	private final Node node;
	private final Graph graph;
	private final GraphEditor editor;


	private Process process;
	private Exchange exchange;

	/**
	 * Create a command that will remove the exchange item from its parent.
	 *
	 * @param parent the parent containing the child
	 * @param child  the component to remove
	 * @throws IllegalArgumentException if any parameter is null
	 */
	public DeleteExchangeCommand(IOPane parent, ExchangeItem child) {
		if (parent == null || child == null) {
			throw new IllegalArgumentException();
		}
		setLabel(NLS.bind(M.Delete.toLowerCase(), M.Flow));
		this.parent = parent;
		this.child = child;
		node = child.getNode();
		graph = node.getGraph();
		editor = graph.getEditor();
	}

	@Override
	public boolean canExecute() {
		if (node == null || node.descriptor == null
				|| node.descriptor.type != ModelType.PROCESS
				|| child.exchange == null
				|| node.descriptor.isFromLibrary())
			return false;

		process = (Process) node.getEntity();
		exchange = getExchange();

		return process != null && exchange != null;
	}

	@Override
	public void execute() {
		redo();
	}

	@Override
	public void redo() {
		// remove the child and disconnect its links
		if (exchange == null)
			return;

		try {
			if (editor.promptSaveIfNecessary())
				delete();
		} catch (Exception e) {
			ErrorReporter.on(
					"failed to update database", e);
		}
	}

	private void delete() {
		if (process == null || exchange == null)
			return;

		var exchanges = List.of(exchange);
		if (!checkRefFlow(process, exchanges))
			return;

		if (exchange.flow == null)
			return;

		if (exchange.flow.flowType != FlowType.ELEMENTARY_FLOW) {
			var usages = new ExchangeUseSearch(Database.get(), process)
					.findUses(List.of(exchange));
			if (!usages.isEmpty()) {
				MsgBox.error(M.CannotRemoveExchanges,
						M.ExchangesAreUsedOrNotDisconnected);
				return;
			}

			if (!checkProviderLinks(process, exchanges, List.of(exchange)))
				return;

			var b = Question.ask("Remove exchange",
					"Remove flow " + Labels.name(exchange.flow)
							+ " from process " + Labels.name(process) + "?");
			if (!b)
				return;
		}

		process.exchanges.remove(exchange);
		var processLinks = graph.linkSearch.getLinks(process.id);
		for (var link : processLinks) {
			if (link.exchangeId == exchange.id) {
				graph.removeLink(link);
			}
		}

		parent.removeChild(child);

		editor.setDirty(process);
	}

	private Exchange getExchange() {
		if (process == null)
			return null;
		return process.exchanges.stream()
				.filter(e -> Objects.equals(e, child.exchange))
				.findFirst()
				.orElse(null);
	}

}
