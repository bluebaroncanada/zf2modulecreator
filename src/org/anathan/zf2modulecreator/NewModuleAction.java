package org.anathan.zf2modulecreator;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.CommandListener;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import com.intellij.util.PlatformIcons;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import de.espend.idea.php.annotation.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Created by Alan on 6/4/2015.
 */
public class NewModuleAction extends DumbAwareAction {

	public NewModuleAction() {
		super("Module", "Create new module", PhpIcons.PHP_FILE);
	}


	private static class AddModuleToConfigAction extends WriteCommandAction<String> {

		private String moduleName;
		private Project project;

		public AddModuleToConfigAction(@Nullable Project project, @NotNull String moduleName, PsiFile... files) {
			super(project, files);

			this.project = project;
			this.moduleName = moduleName;
		}

		@Override
		protected void run(@NotNull Result<String> result) throws Throwable {
			ZF2Util.addModuleToAppConfig(project, moduleName);
		}
	}


	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {

		final Project project = e.getProject();
		if (project == null) {
			return;
		}

		final String moduleName = Messages.showInputDialog(project, "Enter name of the module", "Create New Module", null);
		if (moduleName != null && !moduleName.isEmpty()) {

			final VirtualFile projectBaseDir = project.getBaseDir();

			VirtualFile dir = projectBaseDir.findFileByRelativePath("module/" + moduleName);
			if (dir != null) {
				Messages.showErrorDialog(project, "A module with that name already exists.", "Error");
				return;
			}

			if (!ZF2Util.isValidModuleName(moduleName)) {
				Messages.showErrorDialog(project, "Invalid module name.", "Error");
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

						String innerViewDirName = moduleName.toLowerCase();

						VirtualFile configDir = ZF2Util.createDirectory(project, newModuleDir, "config");
						if (configDir != null) {

							String configFileContent = ZF2Util.readResourceFileText("/resources/module.config.php");

							if (configFileContent != null) {
								configFileContent = configFileContent.replace("{MODULE_NAME}", moduleName);
								configFileContent = configFileContent.replace("{MODULE_SLUG}", innerViewDirName);
							}

							ZF2Util.createFile(project, configDir, "module.config.php", configFileContent);
						}

						VirtualFile srcDir = ZF2Util.createDirectory(project, newModuleDir, "src");
						if (srcDir != null) {

							VirtualFile innerModuleDir = ZF2Util.createDirectory(project, srcDir, moduleName);
							if (innerModuleDir != null) {
								ZF2Util.createDirectory(project, innerModuleDir, "Controller");
								ZF2Util.createDirectory(project, innerModuleDir, "Model");
								ZF2Util.createDirectory(project, innerModuleDir, "Service");
							}

						}

						VirtualFile viewDir = ZF2Util.createDirectory(project, newModuleDir, "view");
						if (viewDir != null) {


							ZF2Util.createDirectory(project, viewDir, innerViewDirName);
						}

						String moduleFileContent = ZF2Util.readResourceFileText("/resources/Module.php");
						if (moduleFileContent != null) {
							moduleFileContent = moduleFileContent.replace("{MODULE_NAME}", moduleName);

							ZF2Util.createFile(project, newModuleDir, "Module.php", moduleFileContent);
						}


						VirtualFile appConfigFile = projectBaseDir.findFileByRelativePath("config/application.config.php");
						if (appConfigFile != null) {
							PsiFile appConfigPsiFile = PsiManager.getInstance(project).findFile(appConfigFile);

							Document doc = PsiDocumentManager.getInstance(project).getDocument(appConfigPsiFile);
							if (doc != null) {

								CommandProcessor.getInstance().executeCommand(project, new Runnable() {
									@Override
									public void run() {
										ZF2Util.addModuleToAppConfig(project, moduleName);
									}
								}, null, null, doc);
							}
						}

						if (configDir != null) {
							VirtualFile configFile = configDir.findChild("module.config.php");
							if (configFile != null) {

								PsiFile configPsiFile = PsiManager.getInstance(project).findFile(configFile);

								if (configPsiFile != null) {

									((Navigatable) configPsiFile.getNavigationElement()).navigate(true);

									Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();

									if (editor != null) {
										editor.getCaretModel().moveCaretRelatively(2, 0, false, false, false);
									}
								}
							}
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
