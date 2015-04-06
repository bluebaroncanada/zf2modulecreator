package org.anathan.zf2modulecreator;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.PlatformIcons;
import com.jetbrains.php.PhpIcons;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Created by Alan on 6/4/2015.
 */
public class NewModuleAction extends DumbAwareAction {

	public NewModuleAction() {
		super("Module", "Create new module", PhpIcons.PHP_FILE);
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {
		final Project project = e.getProject();

		final String moduleName = Messages.showInputDialog(project, "Enter name of the module", "Create New Module", null);
		if (moduleName != null && !moduleName.isEmpty()) {

			final VirtualFile projectBaseDir = project.getBaseDir();

			VirtualFile dir = projectBaseDir.findFileByRelativePath("module/" + moduleName);
			if (dir != null) {
				Messages.showMessageDialog(project, "A module with that name already exists.", "Error", PlatformIcons.ERROR_INTRODUCTION_ICON);
				return;
			}

			ApplicationManager.getApplication().runWriteAction(new Runnable() {
				@Override
				public void run() {
					VirtualFile moduleDir = projectBaseDir.findChild("module");

					if (moduleDir == null) {
						return;
					}

					VirtualFile newModuleDir = ZF2Util.createDirectory(project, moduleDir, moduleName);
					if (newModuleDir != null) {
						VirtualFile configDir = ZF2Util.createDirectory(project, newModuleDir, "config");

						if (configDir != null) {

						}
					}
				}
			});
		}
	}

	public void update(@NotNull AnActionEvent event) {
		Project project = event.getData(PlatformDataKeys.PROJECT);
		if (project == null || !ZF2Util.isZF2Project(project)) {
			setStatus(event, false);
		}
	}

	protected void setStatus(AnActionEvent event, boolean status) {
		event.getPresentation().setVisible(status);
		event.getPresentation().setEnabled(status);
	}
}
