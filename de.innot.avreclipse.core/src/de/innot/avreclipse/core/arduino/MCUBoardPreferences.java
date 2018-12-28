package de.innot.avreclipse.core.arduino;

import java.util.HashMap;
import java.util.Map;

public class MCUBoardPreferences {
	
	public static final String PREF_NAME = "name";
	public static final String PREF_UPLOAD = "upload";
	public static final String PREF_BOOTLOADER = "bootloader";
	public static final String PREF_BUILD = "build";
	
	public static final String PREF_UPLOAD_TOOL = PREF_UPLOAD +".tool";
	public static final String PREF_UPLOAD_PROTOCOL = PREF_UPLOAD +".protocol";
	public static final String PREF_UPLOAD_MAXIMUM_SIZE = PREF_UPLOAD +".maximum_size";
	public static final String PREF_UPLOAD_MAXIMUM_DATA_SIZE = PREF_UPLOAD +".maximum_data_size";
	public static final String PREF_UPLOAD_SPEED = PREF_UPLOAD +".speed";
	public static final String PREF_UPLOAD_DISABLE_FLUSHING = PREF_UPLOAD +".disable_flushing";
	public static final String PREF_UPLOAD_USE_1200BPS_TOUCH = PREF_UPLOAD +".use_1200bps_touch";
	public static final String PREF_UPLOAD_WAIT_FOR_UPLOAD_PORT = PREF_UPLOAD +".wait_for_upload_port";
//	public static final String PREF_UPLOAD_ = PREF_UPLOAD +".";
	
	public static final String PREF_BOOTLOADER_TOOL = PREF_BOOTLOADER + ".tool";
	public static final String PREF_BOOTLOADER_LOW_FUSES = PREF_BOOTLOADER + ".low_fuses";
	public static final String PREF_BOOTLOADER_HIGH_FUSES = PREF_BOOTLOADER + ".high_fuses";
	public static final String PREF_BOOTLOADER_EXTENDED_FUSES = PREF_BOOTLOADER + ".extended_fuses";
	public static final String PREF_BOOTLOADER_FILE = PREF_BOOTLOADER + ".file";
	public static final String PREF_BOOTLOADER_NOBLINK = PREF_BOOTLOADER + ".noblink";
	public static final String PREF_BOOTLOADER_UNLOCK_BITS = PREF_BOOTLOADER + ".unlock_bits";
	public static final String PREF_BOOTLOADER_LOCK_BITS = PREF_BOOTLOADER + ".lock_bits";
//	public static final String PREF_BOOTLOADER_ = PREF_BOOTLOADER + ".";

	public static final String PREF_BUILD_MCU = PREF_BUILD + ".mcu";
	public static final String PREF_BUILD_F_CPU = PREF_BUILD + ".f_cpu";
	public static final String PREF_BUILD_VID = PREF_BUILD + ".vid";
	public static final String PREF_BUILD_PID = PREF_BUILD + ".pid";
	public static final String PREF_BUILD_USB_PRODUCT = PREF_BUILD + ".usb_product";
	public static final String PREF_BUILD_BOARD = PREF_BUILD + ".board";
	public static final String PREF_BUILD_CORE = PREF_BUILD + ".core";
	public static final String PREF_BUILD_VARIANT = PREF_BUILD + ".variant";
	public static final String PREF_BUILD_EXTRA_FLAGS = PREF_BUILD + ".extra_flags";
//	public static final String PREF_BUILD_ = PREF_BUILD + ".";
	
	private String name;
	
	private Map<String, String> upload;
	
	private Map<String, String> bootloader;
	
	private Map<String, String> build;
	
	private Map<String, String> unqualified;

	public MCUBoardPreferences() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, String> getUpload() {
		if (upload == null) {
			upload = new HashMap<String, String>();
		}
		return upload;
	}

	public Map<String, String> getBootloader() {
		if (bootloader == null) {
			bootloader = new HashMap<String, String>();
		}
		return bootloader;
	}

	public Map<String, String> getBuild() {
		if (build == null) {
			build = new HashMap<String, String>();
		}
		return build;
	}

	public Map<String, String> getUnqualified() {
		if (unqualified == null) {
			unqualified = new HashMap<String, String>();
		}
		return unqualified;
	}

	public String getPreference(String name) {
		String[] names = name.split("[.]");
		if (names.length == 2) {
			if (PREF_UPLOAD.equals(names[0])) {
				return getUpload().get(names[1]);
			} else if (PREF_BOOTLOADER.equals(names[0])) {
				return getBootloader().get(names[1]);
			} else if (PREF_BUILD.equals(names[0])) {
				return getBuild().get(names[1]);
			} else {
				return getUnqualified().get(name);
			}
		} else {
			return getUnqualified().get(name);
		}
	}
	
	public void putPreference(String name, String value) {
		String[] names = name.split("[.]");
		if (names.length == 2) {
			if (PREF_UPLOAD.equals(names[0])) {
				getUpload().put(names[1], value);
			} else if (PREF_BOOTLOADER.equals(names[0])) {
				getBootloader().put(names[1], value);
			} else if (PREF_BUILD.equals(names[0])) {
				getBuild().put(names[1], value);
			} else {
				getUnqualified().put(names[1], value);
			}
		} else {
			getUnqualified().put(name, value);
		}
	}

}
