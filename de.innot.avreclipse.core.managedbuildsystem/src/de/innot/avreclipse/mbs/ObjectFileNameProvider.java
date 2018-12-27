package de.innot.avreclipse.mbs;

import org.eclipse.cdt.managedbuilder.core.IManagedOutputNameProvider;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class ObjectFileNameProvider implements IManagedOutputNameProvider {

	@Override
	public IPath[] getOutputNames(ITool tool, IPath[] primaryInputNames) {
		if (primaryInputNames != null) {
			if (primaryInputNames.length > 0) {
				IPath inName = primaryInputNames[0];
				return new IPath[] {new Path(inName.lastSegment() + ".o")};
			} else {
				return new IPath[0];
			}
		} else {
			return null;
		}
	}

}
