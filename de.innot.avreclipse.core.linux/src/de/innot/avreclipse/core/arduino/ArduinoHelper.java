package de.innot.avreclipse.core.arduino;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.utils.spawner.ProcessFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import de.innot.avreclipse.AVRPlugin;
import de.innot.avreclipse.core.arduino.BoardPreferences.VidPid;

public class ArduinoHelper extends AbstractArduinoHelper {
	
	public static final String TTY_USB_PATTERN		= "/dev/ttyUSB*";
	public static final String TTY_ACM_PATTERN		= "/dev/ttyACM*";
	public static final String UDEVINFO_PATTERN		= "([a-zA-Z])[:]\\s*([a-zA-Z_]+)[=]([0-9a-fA-F]+)";
	public static final String UDEVINFO_ID_VENDOR	= "ID_VENDOR_ID";
	public static final String UDEVINFO_ID_MODEL	= "ID_MODEL_ID";

	public ArduinoHelper() {
	}

	@Override
	public List<String> findArduinoPorts(ArduinoBoards boards, String boardId) {
		MCUBoardPreferences prefs = boards.getMCUMap().get(boardId);
		if (prefs != null) {
			List<String> result = new ArrayList<String>();
			for (String usbFile : findPresentUSBFiles()) {
				VidPid vidpid = getVidPidForUSBFile(usbFile);
				for (VidPid requiredVidPid : prefs.getVidPidList()) {
					if (VidPid.COMPARATOR.compare(vidpid, requiredVidPid) == 0) {
						result.add(usbFile);
					}
				}
			}
			return result;
		}
		return null;
	}
	
	private List<VidPid> findPresentUSBDevices(MCUBoardPreferences prefs) {
		List<VidPid> result = new ArrayList<VidPid>();
		List<VidPid> requiredUSBList = prefs.getVidPidList();
		List<VidPid> presentUSBList = enumerateUSBDevices();
		for (VidPid presentUSB : presentUSBList) {
			for (VidPid requiredUSB : requiredUSBList) {
				if (VidPid.COMPARATOR.compare(requiredUSB, presentUSB) == 0) {
					result.add(requiredUSB);
				}
			}
		}
		return result;
	}

	private List<VidPid> enumerateUSBDevices() {
		List<VidPid> result = new ArrayList<BoardPreferences.VidPid>();
		Context context = new Context();
		int res = LibUsb.init(context);
		if (res != LibUsb.SUCCESS) {
			throw new LibUsbException("Unable to initialize libusb.", res);
		} else {
			DeviceList list = new DeviceList();
			res = LibUsb.getDeviceList(null, list);
			if (res < 0) {
				throw new LibUsbException("Unable to get device list", res);
			}
			
			try {
				// Iterate over all devices and scan for the right one
				for (Device device : list) {
					DeviceDescriptor descriptor = new DeviceDescriptor();
					res = LibUsb.getDeviceDescriptor(device, descriptor);
					if (res != LibUsb.SUCCESS) {
						throw new LibUsbException("Unable to read device descriptor", res);
					}
					result.add(new VidPid(Integer.valueOf(descriptor.idVendor()), Integer.valueOf(descriptor.idProduct())));
				}
			} finally {
				// Ensure the allocated device list is freed
				LibUsb.freeDeviceList(list, true);
			}

		}
		LibUsb.exit(context);
		return result;
	}
	
	private List<String> findPresentUSBFiles() {
		List<String> result = new ArrayList<String>();
//		result.addAll(executeCommand("ls", "-1", TTY_ACM_PATTERN));
//		result.addAll(executeCommand("ls", "-1", TTY_USB_PATTERN));
		File devDir = new File("/dev");
		if (devDir.isDirectory()
				&& devDir.canRead()) {
			for (File f : devDir.listFiles()) {
				if (f.getName().startsWith("ttyACM")
								|| f.getName().startsWith("ttyUSB")) {
					result.add(f.getAbsolutePath());
				}
			}
		}
		return result;
	}
	
	private VidPid getVidPidForUSBFile(String usbFile) {
		VidPid result = new VidPid();
		Pattern pattern = Pattern.compile(UDEVINFO_PATTERN);
		for (String udevInfo : executeCommand("udevadm", "info", usbFile)) {
			Matcher matcher = pattern.matcher(udevInfo);
			if (matcher.matches()
					&& (matcher.groupCount() == 3)) {
				if (UDEVINFO_ID_VENDOR.equals(matcher.group(2))) {
					result.vid = Integer.valueOf(matcher.group(3), 16);
				} else if (UDEVINFO_ID_MODEL.equals(matcher.group(2))) {
					result.pid = Integer.valueOf(matcher.group(3), 16);
				}
			}
		}
		return ((result.vid != null) && (result.pid != null) ?
					result :
						null);
	}
	
	public List<String> executeCommand(String... command) {

		List<String> result = new ArrayList<String>();
		
		Process cmdproc = null;
		InputStream is = null;
		InputStreamReader isr = null;
		BufferedReader br = null;

		try {
			cmdproc = ProcessFactory.getFactory().exec(command);
			
			is = cmdproc.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			
			String line;

			while ((line = br.readLine()) != null) {
				if (line.length() > 1) {
					result.add(line);
				}
			}
			
		} catch (IOException ex) {
			AVRPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, AVRPlugin.PLUGIN_ID, "Error while executing command: " + command, ex));
		} finally {
			try {
				if (br != null)
					br.close();
				if (isr != null)
					isr.close();
				if (is != null)
					is.close();
			} catch (IOException e) {
				// can't do anything about it
			}
			try {
				if (cmdproc != null) {
					cmdproc.waitFor();
				}
			} catch (InterruptedException e) {
			}
		}

		return result;
	}


}
