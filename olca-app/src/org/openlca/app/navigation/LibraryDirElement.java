package org.openlca.app.navigation;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.openlca.app.rcp.Workspace;
import org.openlca.core.library.LibraryDir;

public class LibraryDirElement extends NavigationElement<LibraryDir> {

	private Set<String> only;

	LibraryDirElement(INavigationElement<?> parent, LibraryDir dir) {
		super(parent, dir);
	}

	LibraryDirElement only(Set<String> ids) {
		this.only = ids;
		return this;
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		if (only != null) {
			return only.stream()
					.map(id -> Workspace.getLibraryDir().get(id))
					.filter(Optional::isPresent)
					.map(lib -> new LibraryElement(this, lib.get()))
					.collect(Collectors.toList());
		}
		return getContent()
				.getLibraries()
				.stream()
				.map(lib -> new LibraryElement(this, lib))
				.collect(Collectors.toList());
	}
}
