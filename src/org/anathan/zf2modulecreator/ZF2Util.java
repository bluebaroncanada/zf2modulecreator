package org.anathan.zf2modulecreator;

import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFileFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Created by Alan on 6/4/2015.
 */
public class ZF2Util {


	public static boolean isZF2Project(@NotNull Project project) {
		VirtualFile baseDir = project.getBaseDir();

		VirtualFile zfDir = baseDir.findFileByRelativePath("vendor/zendframework");
		if (zfDir == null || !zfDir.isDirectory()) {
			return false;
		}

		VirtualFile moduleDir = baseDir.findFileByRelativePath("module");
		if (moduleDir == null || !moduleDir.isDirectory()) {
			return false;
		}

		VirtualFile appConfigFile = baseDir.findFileByRelativePath("config/application.config.php");
		if (appConfigFile == null || appConfigFile.isDirectory()) {
			return false;
		}

		return true;
	}

	public static VirtualFile createDirectory(@NotNull Project project, @NotNull VirtualFile baseDir, @NotNull String dirName) {
		try {
			return baseDir.createChildDirectory(project, dirName);
		} catch (IOException e1) {
			//
		}
		return null;
	}
}
