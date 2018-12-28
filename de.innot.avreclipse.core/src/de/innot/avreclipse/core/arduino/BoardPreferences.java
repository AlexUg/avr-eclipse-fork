package de.innot.avreclipse.core.arduino;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoardPreferences extends MCUBoardPreferences {
	
	public static final String PREF_MENU_CPU = "menu.cpu";
	public static final String PREF_VID = "vid";
	public static final String PREF_PID = "pid";
	
	public static class VidPid {
		public Integer vid;
		public Integer pid;

		@Override
		public String toString() {
			return (vid == null ? "<null>" : String.format("%1$04X", vid))
					+ ":"
					+ (pid == null ? "<null>" : String.format("%1$04X", pid));
		}
	}
	
	private class MCUBoardPreferencesInternal extends MCUBoardPreferences {
		
		@Override
		public String getName() {
			StringBuilder result = new StringBuilder(BoardPreferences.this.getName());
			result.append(" (");
			result.append(super.getName());
			result.append(')');
			return result.toString();
		}

		@Override
		public String getPreference(String name) {
			String result = super.getPreference(name);
			if (result == null) {
				result = BoardPreferences.this.getPreference(name);
			}
			return result;
		}
	}
	
	private Map<String, MCUBoardPreferences> mcuMap;
	
	private List<VidPid> vidpidList;
	
	private Map<VidPid, MCUBoardPreferences> vidpidMap;

	public BoardPreferences() {
	}

	public Map<String, MCUBoardPreferences> getMcuMap() {
		if (mcuMap == null) {
			mcuMap = new HashMap<String, MCUBoardPreferences>();
		}
		return mcuMap;
	}

	public List<VidPid> getVidPidList() {
		if (vidpidList == null) {
			vidpidList = new ArrayList<VidPid>();
		}
		return vidpidList;
	}

	public Map<VidPid, MCUBoardPreferences> getVidPidMap() {
		if (vidpidMap == null) {
			vidpidMap = new HashMap<VidPid, MCUBoardPreferences>();
		}
		return vidpidMap;
	}

	@Override
	public void putPreference(String name, String value) {
		if (name.startsWith(PREF_VID)) {
			name = name.substring(PREF_VID.length() + 1);
			putVidPidPreference(PREF_VID, name, value);
		} else if (name.startsWith(PREF_PID)) {
			name = name.substring(PREF_PID.length() + 1);
			putVidPidPreference(PREF_PID, name, value);
		} else if (name.startsWith(PREF_MENU_CPU)) {
			name = name.substring(PREF_MENU_CPU.length() + 1);
			String mcuId;
			int idx = name.indexOf('.');
			if (idx > 0) {
				mcuId = name.substring(0, idx);
				name = name.substring(idx + 1);
			} else {
				mcuId = name;
				name = "";
			}
			MCUBoardPreferences mcuPrefs = getMcuMap().get(mcuId);
			if (mcuPrefs == null) {
				mcuPrefs = new MCUBoardPreferencesInternal();
				getMcuMap().put(mcuId, mcuPrefs);
			}
			if (name.isEmpty()) {
				mcuPrefs.setName(value);
			} else {
				mcuPrefs.putPreference(name, value);
			}
		} else {
			super.putPreference(name, value);
		}
	}
	
	private void putVidPidPreference(String type, String name, String value) {
		int index = -1;
		int idx = name.indexOf('.');
		if (idx > 0) {
			index = Integer.valueOf(name.substring(0, idx));
			name = name.substring(idx + 1);
		} else {
			index = Integer.valueOf(name);
			name = "";
		}
		List<VidPid> list = getVidPidList();
		while (index >= list.size()) {
			list.add(new VidPid());
		}
		VidPid vidpid = list.get(index);
		if (name.isEmpty()) {
			int radix = 10;
			if (value.startsWith("0x")) {
				value = value.substring(2);
				radix = 16;
			}
			if (PREF_VID.equals(type)) {
				vidpid.vid = Integer.valueOf(value, radix);
			} else if (PREF_PID.equals(type)) {
				vidpid.pid = Integer.valueOf(value, radix);
			}
		} else {
			MCUBoardPreferences prefsInt = getVidPidMap().get(vidpid);
			if (prefsInt == null) {
				prefsInt = new MCUBoardPreferencesInternal();
				getVidPidMap().put(vidpid, prefsInt);
			}
			prefsInt.putPreference(name, value);
		}
	}

}
