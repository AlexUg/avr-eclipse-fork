package de.innot.avreclipse.core.arduino;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import de.innot.avreclipse.AVRPlugin;
import de.innot.avreclipse.core.avrdude.ProgrammerConfig;


/**
 * 
 * @author Aleksandr Ugnenko
 *
 */
public class ProjectConfigurator {
	
	private static final String AVR_C_COMPILER_TOOL		= "de.innot.avreclipse.tool.compiler.winavr";
	private static final String AVR_CPP_COMPILER_TOOL	= "de.innot.avreclipse.tool.cppcompiler";
	
	private static final String AVR_C_INCLUDE_OPTION	= "de.innot.avreclipse.compiler.option.incpath";
	private static final String AVR_CPP_INCLUDE_OPTION	= "de.innot.avreclipse.cppcompiler.option.incpath";
	
	private static final String INC_PATH_PREFIX			= "\"${workspace_loc:/${ProjName}/";
	
	private static final String DEFAULT_SCETCH_NAME		= "scetch.cpp";
	private static final String DEFAULT_SCETCH_PATH		= "/resources/" + DEFAULT_SCETCH_NAME;
	
	
	public static final String CORE_PREFFIX			= "core_";
	
	public static final String CORE_CHECK_FILE			= "Arduino.h";
	public static final String VARIANT_CHECK_FILE		= "pins_arduino.h";
	
	private static final Map<String, ProgrammerConfig> progConfigMap = new HashMap<String, ProgrammerConfig>();
	

	public static boolean checkCoreFolder(IFolder folder) {
		return checkArduinoFolder(folder, new Path(CORE_CHECK_FILE));
	}
	
	public static boolean checkVariantFolder(IFolder folder) {
		return checkArduinoFolder(folder, new Path(VARIANT_CHECK_FILE));
	}
	
	public static boolean checkArduinoFolder(IFolder folder, IPath path) {
		if (folder.exists()) {
			return folder.getFile(path).isAccessible();
		}
		return false;
	}

	public static void linkArduinoSourceFolder(ICProjectDescription pDesc, String folderPathStr, String folderNameStr) throws CoreException {
		if (folderPathStr != null) {
			Path folderPath = new Path(folderPathStr);
			linkArduinoSourceFolder(pDesc, folderPath, folderNameStr);
		}
	}
	
	public static void linkArduinoSourceFolder(ICProjectDescription pDesc, IPath folderPath, String folderNameStr) throws CoreException {
		if (folderPath != null) {
			
			if ((folderNameStr == null)
					|| folderNameStr.isEmpty()) {
				folderNameStr = folderPath.lastSegment();
			}
			
			IFolder folder = createArduinoSourceFolder(pDesc.getProject(), folderPath, folderNameStr);
			
			for (ICConfigurationDescription confDesc : pDesc.getConfigurations()) {
				IConfiguration cfg = ManagedBuildManager.getConfigurationForDescription(confDesc);
				for (ITool t : cfg.getToolChain().getTools()) {
					ITool condidate = t;
					do {
						if (AVR_C_COMPILER_TOOL.equals(t.getId())
								|| AVR_CPP_COMPILER_TOOL.equals(t.getId())) {
							configureTool(cfg, condidate, folder);
							break;
						}
					} while ((t = t.getSuperClass()) != null);
				}
			}
		}
	}
	
	public static IFolder createArduinoSourceFolder(IProject project, IPath folderPath, String folderNameStr) throws CoreException {
		try {
			IFolder folder = project.getFolder(new Path(folderNameStr));
			if (folder.isAccessible()) {
				if (folder.isLinked()) {
					folder.delete(true, null);
				} else if (!folder.isPhantom()) {
					if (folderNameStr.startsWith(CORE_PREFFIX)) {
						int idx = folderNameStr.lastIndexOf('_');
						if (idx > 0) {
							try {
								return createArduinoSourceFolder(project,
																	folderPath,
																	folderNameStr.substring(0, idx)
																		+ '_'
																		+ String.valueOf(Integer.valueOf(folderNameStr.substring(idx + 1)) + 1));
							} catch (Exception ex) {
								idx = 1;
							}
						} else {
							idx = 1;
						}
						return createArduinoSourceFolder(project, folderPath, folderNameStr + '_' + String.valueOf(idx));
					} else {
						return createArduinoSourceFolder(project, folderPath, CORE_PREFFIX + folderNameStr);
					}
				}
			}
			folder.createLink(folderPath, 0, null);
			return folder;
		} catch (CoreException ex) {
			AVRPlugin.getDefault().getLog().log(ex.getStatus());
			throw ex;
		}
	}
	
	public static void addScetch(IProject project) throws CoreException {
		URL defScetchURL = AVRPlugin.getDefault().getBundle().getEntry(DEFAULT_SCETCH_PATH);
		IFile srcFile = project.getFile(DEFAULT_SCETCH_NAME);
		if (!srcFile.exists()) {
			InputStream defIn = null;
			try {
				defIn = defScetchURL.openStream();
				srcFile.create(defIn, true, null);
				project.refreshLocal(IResource.DEPTH_ONE, null);
			} catch (IOException ex) {
				throw new CoreException(new Status(IStatus.ERROR,
													AVRPlugin.PLUGIN_ID,
													"Error while reading scetch template by URL: " + defScetchURL.toString(),
													ex));
			} finally {
				if (defIn != null) {
					try {
						defIn.close();
					} catch (IOException e) {
					}
				}
			}
		}
	}
	
	public static void configureForArduino(ICProjectDescription pDesc, String boardId) throws CoreException {
		ArduinoBoards boardPreferences = ArduinoBoards.getInstance();
		if (boardPreferences != null) {
			IProject project = pDesc.getProject();
			IPath ardPath = boardPreferences.getArduinoPath();
			if (ardPath != null) {
				IPath corePath = ardPath.append("cores/arduino");
				IFolder coreFolder = project.getFolder(corePath.lastSegment());
				if (corePath.toFile().isDirectory()) {
					if ((coreFolder == null)
							|| !checkCoreFolder(coreFolder)) {
						// Link Arduino core sources
						linkArduinoSourceFolder(pDesc, corePath, corePath.lastSegment());
					}
				} else {
					throw new CoreException(new Status(IStatus.ERROR,
														AVRPlugin.PLUGIN_ID,
														String.format("Path '%1$s' isn't directory with Arduino core sources. Check path 'Arduino' in AVR preferences",
																		corePath.toOSString())
														)
											);
				}
				IPath variantPath = ardPath.append("variants");
				variantPath = variantPath.append(boardPreferences.getVariant(boardId));
				IFolder variantFolder = getVariantFolder(project, variantPath.lastSegment());
				if (variantPath.toFile().isDirectory()) {
					if ((variantFolder == null)
							|| !checkVariantFolder(variantFolder)) {
						linkArduinoSourceFolder(pDesc, variantPath, variantPath.lastSegment());
					}
				} else {
					throw new CoreException(new Status(IStatus.ERROR,
														AVRPlugin.PLUGIN_ID,
														String.format("Path '%1$s' isn't directory with Arduino variant sources. Check path 'Arduino' in AVR preferences",
																		variantPath.toOSString())
														)
											);
				}
				fixIncludes(pDesc, coreFolder, variantFolder);
				addScetch(project);
				project.refreshLocal(IResource.DEPTH_INFINITE, null);
			} else {
				throw new CoreException(new Status(IStatus.ERROR,
													AVRPlugin.PLUGIN_ID,
													"Arduino sources aren't defined. Check path 'Arduino' in AVR preferences"
													)
										);
			}
		}
	}
	
	public static ProgrammerConfig getArduinoProgrammer(String boardId) {
		ProgrammerConfig result = progConfigMap.get(boardId);
		if (result == null) {
			result = createArduinoProgrammer(boardId);
			if (result != null) {
				progConfigMap.put(boardId, result);
			}
		}
		return result;
	}
	
	public static ProgrammerConfig createArduinoProgrammer(String boardId) {
		if ((boardId != null)
				&& !boardId.isEmpty()) {
			ArduinoBoards boardPreferences = ArduinoBoards.getInstance();
			if (boardPreferences != null) {
				ProgrammerConfig result = new ProgrammerConfig(true, boardId);
				result.setProgrammer(boardPreferences.getBoardPreference(boardId, MCUBoardPreferences.PREF_UPLOAD_PROTOCOL));
				result.setBaudrate(boardPreferences.getBoardPreference(boardId, MCUBoardPreferences.PREF_UPLOAD_SPEED));
				result.setUse1200bpsTouch(boardPreferences.getBoardPreference(boardId, MCUBoardPreferences.PREF_UPLOAD_USE_1200BPS_TOUCH));
				result.setWaitForUploadPort(boardPreferences.getBoardPreference(boardId, MCUBoardPreferences.PREF_UPLOAD_WAIT_FOR_UPLOAD_PORT));
				return result;
			}
		}
		return null;
	}
	
	/*
	 * This implementation must to be replaced with Plugin Fragment for specific OS.
	 */
	public static List<String> findArduinoPorts(String boardId) {
		return AVRPlugin.getDefault().getArduinoHelper().findArduinoPorts(ArduinoBoards.getInstance(), boardId);
	}
	
	public static void fixIncludes(ICProjectDescription pDesc, IFolder... linkedFolders) {
		for (ICConfigurationDescription confDesc : pDesc.getConfigurations()) {
			IConfiguration cfg = ManagedBuildManager.getConfigurationForDescription(confDesc);
			for (ITool t : cfg.getToolChain().getTools()) {
				ITool condidate = t;
				do {
					if (AVR_C_COMPILER_TOOL.equals(t.getId())
							|| AVR_CPP_COMPILER_TOOL.equals(t.getId())) {
						if (linkedFolders != null) {
							configureTool(cfg, condidate, linkedFolders);
						}
						configureTool(pDesc.getProject(), cfg, condidate);
						break;
					}
				} while ((t = t.getSuperClass()) != null);
			}
		}
	}
	
	private static IFolder getVariantFolder(IProject project, String expectedVariant) throws CoreException {
		IFolder result = null;
		for (IResource rsrc : project.members()) {
			if (rsrc instanceof IFolder) {
				IFolder condidate = (IFolder) rsrc;
				if (condidate.getFile(VARIANT_CHECK_FILE).isAccessible()) {
					if (condidate.getName().equals(expectedVariant)) {
						result = condidate;
					} else {
						condidate.delete(true, null);
					}
				}
			}
		}
		return result;
	}
	
	private static boolean configureTool(IConfiguration config, ITool tool, IFolder... linkedFolders) {
		for (IOption opt : tool.getOptions()) {
			IOption condidate = opt;
			do {
				if (AVR_C_INCLUDE_OPTION .equals(opt.getId())
						|| AVR_CPP_INCLUDE_OPTION .equals(opt.getId())) {
					Object value = condidate.getValue();
					if (value instanceof List) {
						for (IFolder linkedFolder : linkedFolders) {
							String newValue = INC_PATH_PREFIX + linkedFolder.getProjectRelativePath().toPortableString() + "}\"";
							
							@SuppressWarnings("unchecked")
							List<String> listValue = (List<String>) value;
							if (listValue.contains(newValue)) {
								continue;
							}
							listValue.add(newValue);
							try {
								condidate = config.setOption(tool, condidate, listValue.toArray(new String[listValue.size()]));
							} catch (BuildException e) {
							}
						}
						return true;
					} else {
						AVRPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, AVRPlugin.PLUGIN_ID, "Wrong value type for option: " + opt.getName()));
					}
				}
			} while ((opt = opt.getSuperClass()) != null);
		}
		return false;
	}
	

	private static boolean configureTool(IProject project, IConfiguration config, ITool tool) {
		boolean result = false;
		for (IOption opt : tool.getOptions()) {
			IOption condidate = opt;
			do {
				if (AVR_C_INCLUDE_OPTION .equals(opt.getId())
						|| AVR_CPP_INCLUDE_OPTION .equals(opt.getId())) {
					Object value = condidate.getValue();
					if (value instanceof List) {
						
						@SuppressWarnings("unchecked")
						List<String> listValue = (List<String>) value;
						Iterator<String> iter = listValue.iterator();
						while (iter.hasNext()) {
							String incFolder = (String) iter.next();
							if (incFolder.startsWith(INC_PATH_PREFIX)) {
								incFolder = incFolder.substring(INC_PATH_PREFIX.length());
								if (incFolder.endsWith("}\"")) {
									incFolder = incFolder.substring(0, incFolder.length() - 2);
								}
								IFolder folder = project.getFolder(new Path(incFolder));
								if (!folder.exists()) {
									iter.remove();
									result = true;
								}
							}
						}
						try {
							condidate = config.setOption(tool, condidate, listValue.toArray(new String[listValue.size()]));
						} catch (BuildException e) {
						}
					} else {
						AVRPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, AVRPlugin.PLUGIN_ID, "Wrong value type for option: " + opt.getName()));
					}
				}
			} while ((opt = opt.getSuperClass()) != null);
		}
		return result;
	}
}
