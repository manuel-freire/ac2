package es.ucm.fdi.ac.gui;

/**
 * Returns the version-string to display on window titles
 *
 * Created by mfreire on 3/10/16.
 */
public class ACVersion {

	private static String version;

	public static String getVersion() {
		if (version == null) {
			// relies on the Implementation-Version META-INF/MANIFEST.MF property
			String v = MainGui.class.getPackage().getImplementationVersion();
			version = (v == null || v.isEmpty()) ? "[dev]" : v;
			System.err.println("Version: " + version);
		}
		return version;
	}
}
