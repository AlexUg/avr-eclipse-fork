package de.innot.avreclipse.core.arduino;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import de.innot.avreclipse.AVRPlugin;
import de.innot.avreclipse.core.arduino.BoardPreferences.VidPid;
import de.innot.avreclipse.core.paths.AVRPath;

public class ArduinoBoards {
	
	public static final String PREFERENCE_UI_PATH = "Window -> Preferences -> AVR -> Paths";
	
	private static ArduinoBoards INSTANCE;
	
	public static IStatus LAST_ERROR = null;
	
	private Map<String, BoardPreferences> boardsMap;
	
	private Map<String, MCUBoardPreferences> mcuMap;
	
	public static ArduinoBoards getInstance() {
		if (INSTANCE == null) {
			IPath path = AVRPath.ARDUINO.getPathManager().getPath();
			if ((path != null)
					&& !path.isEmpty()) {
				File boardsTxt = path.append(AVRPath.ARDUINO.getTest()).toFile();
				if (boardsTxt.canRead()) {
					INSTANCE = new ArduinoBoards(boardsTxt);
					LAST_ERROR = null;
				} else {
					LAST_ERROR = new Status(IStatus.ERROR, AVRPlugin.PLUGIN_ID, "Arduino 'boards.txt' can't be read. "
							+ "See '" + PREFERENCE_UI_PATH + "'");
				}
			} else {
				LAST_ERROR = new Status(IStatus.ERROR, AVRPlugin.PLUGIN_ID, "Arduino preferences aren't defined. "
						+ "See '" + PREFERENCE_UI_PATH + "'");
			}
		}
		return INSTANCE;
	}
	
	public static void reset() {
		INSTANCE = null;
	}

	private ArduinoBoards(File boardsTxt) {
		load(boardsTxt);
	}
	
	public Map<String, BoardPreferences> getBoardsMap() {
		if (boardsMap == null) {
			boardsMap = new HashMap<String, BoardPreferences>();
		}
		return boardsMap;
	}
	
	public Map<String, MCUBoardPreferences> getMCUMap() {
		if (mcuMap == null) {
			mcuMap = new HashMap<String, MCUBoardPreferences>();
			for (Entry<String, BoardPreferences> entry : getBoardsMap().entrySet()) {
				String k = entry.getKey();
				BoardPreferences v = entry.getValue();
				if (!v.getMcuMap().isEmpty()) {
					for (Entry<String, MCUBoardPreferences> centry : v.getMcuMap().entrySet()) {
						mcuMap.put(k + "." + BoardPreferences.PREF_MENU_CPU + "." + centry.getKey(),  centry.getValue());
					}
				} else if (!v.getVidPidMap().isEmpty()) {
					for (Entry<VidPid, MCUBoardPreferences> centry : v.getVidPidMap().entrySet()) {
						centry.getValue().setName(centry.getKey().toString());
						mcuMap.put(k + "." + centry.getKey().toString(), centry.getValue());
					}
				} else if ((v.getName() != null)
							&& !v.getName().isEmpty()) {
					mcuMap.put(k, v);
				}
			};
		}
		return mcuMap;
	}
	
	public IPath getArduinoPath() {
		return AVRPath.ARDUINO.getPathManager().getPath();
	}
	
	public String getBoardName(String boardId) {
		MCUBoardPreferences mcuPrefs = getMCUMap().get(boardId);
		if (mcuPrefs != null) {
			return mcuPrefs.getName();
		}
		return "<unknown>";
	}
	
	public String getBoardPreference(String boardId, String name) {
		MCUBoardPreferences mcuPrefs = getMCUMap().get(boardId);
		if (mcuPrefs != null) {
			return mcuPrefs.getPreference(name);
		}
		return "<unknown>";
	}
	
	public String getMCUType(String boardId) {
		return getBoardPreference(boardId, MCUBoardPreferences.PREF_BUILD_MCU);
	}
	
	public String getFCPU(String boardId) {
		return getBoardPreference(boardId, MCUBoardPreferences.PREF_BUILD_F_CPU);
	}
	
	public String getVariant(String boardId) {
		return getBoardPreference(boardId, MCUBoardPreferences.PREF_BUILD_VARIANT);
	}
	
	private void load(File boardsTxt) {
		Properties props = new Properties();
		FileReader reader = null;
		try {
			reader = new FileReader(boardsTxt);
			props.load(reader);
			for (Entry<Object, Object> entry : props.entrySet()) {
				String k = entry.getKey().toString();
				String v = entry.getValue().toString();
				int idx = k.toString().indexOf('.');
				if (idx > 0) {
					String boardId = k.toString().substring(0, idx);
					String name = k.toString().substring(idx + 1);
					BoardPreferences prefs = getBoardsMap().get(boardId);
					if (prefs == null) {
						prefs = new BoardPreferences();
						getBoardsMap().put(boardId, prefs);
					}
					if (MCUBoardPreferences.PREF_NAME.equals(name)) {
						prefs.setName(v.toString());
					} else {
						prefs.putPreference(name, v.toString());
					}
				}
			};
		} catch (IOException ex) {
			throw new RuntimeException("Error while loading file: " + boardsTxt.getAbsolutePath(), ex);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
				}
			}
		}
	}

}
