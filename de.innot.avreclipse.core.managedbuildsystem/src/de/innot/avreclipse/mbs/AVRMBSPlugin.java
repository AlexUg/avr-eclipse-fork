package de.innot.avreclipse.mbs;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class AVRMBSPlugin extends Plugin {
	
	public static final String PLUGIN_ID = "";
	
	// The shared instance
	private static AVRMBSPlugin	plugin;

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}
	
	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static AVRMBSPlugin getDefault() {
		return plugin;
	}

}
