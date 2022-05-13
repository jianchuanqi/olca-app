package org.openlca.app.editors.graph.edit;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.openlca.app.editors.graph.model.ExchangeItem;
import org.openlca.app.editors.graph.model.Graph;
import org.openlca.app.editors.graph.model.IOPane;
import org.openlca.app.editors.graph.model.Node;

/**
 * A class that handles appropriate object creation (the {@link EditPart}s)
 * depending on what is to be obtained, no matter what is the object class.
 */
public class GraphEditPartFactory implements EditPartFactory {

	@Override
	public EditPart createEditPart(EditPart context, Object model) {
		var part = editPartOf(model);
		if (part == null) {
			return null;
		}
		part.setModel(model);
		return part;
	}

	private EditPart editPartOf(Object model) {
		if (model instanceof Graph)
			return new GraphEditPart();
		if (model instanceof Node node)
			if (node.isMinimized())
				return new NodeEditPart.Minimized();
			else return new NodeEditPart.Maximized();
		if (model instanceof IOPane)
			return new IOPaneEditPart();
		if (model instanceof ExchangeItem)
			return new ExchangeItemEditPart();
		return null;
	}

}
