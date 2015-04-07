package org.anathan.zf2modulecreator;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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


	public static void addArrayValueToArrayCreation(@NotNull ArrayCreationExpression arrayCreation, @NotNull String valueText) {

		final Project project = arrayCreation.getProject();
		final PsiFile psiFile = arrayCreation.getContainingFile();

		final ArrayCreationExpression arrayCreationExpression = arrayCreation;
		final String newValueText = valueText;

		final Document doc = PsiDocumentManager.getInstance(project).getDocument(psiFile);
		if (doc == null) {
			return;
		}
		CommandProcessor.getInstance().executeCommand(project, new Runnable() {
			@Override
			public void run() {

				PsiElement[] arrayChildren = arrayCreationExpression.getChildren(); // all are Array Values

				if (arrayChildren.length >= 1) {
					PsiElement lastArrayValue = arrayChildren[arrayChildren.length - 1];
					PsiElement whitespace = lastArrayValue.getPrevSibling(); // this is the white space before last PhpPsiElement
					PsiElement newWhiteSpace = null;
					if (whitespace instanceof PsiWhiteSpace) {
						newWhiteSpace = PsiElementFactory.SERVICE.getInstance(project).createDummyHolder(whitespace.getText(), PhpTokenTypes.WHITE_SPACE, null);
					}

					if (lastArrayValue.getNextSibling().getText().equals(",")) {

						PsiElement comma = lastArrayValue.getNextSibling();
						PsiElement newPsi = PsiElementFactory.SERVICE.getInstance(project).createDummyHolder(newValueText, PhpElementTypes.ARRAY_VALUE, null);

						arrayCreationExpression.addAfter(newPsi, comma);

						if (newWhiteSpace != null) {
							arrayCreationExpression.addAfter(newWhiteSpace, comma);
						}
					} else {
						PsiElement newComma = PhpPsiElementFactory.createComma(project);
						PsiElement newPsi = PsiElementFactory.SERVICE.getInstance(project).createDummyHolder(newValueText, PhpElementTypes.ARRAY_VALUE, null);

						arrayCreationExpression.addAfter(newPsi, lastArrayValue);

						if (newWhiteSpace != null) {
							arrayCreationExpression.addAfter(newWhiteSpace, lastArrayValue);
						}

						arrayCreationExpression.addAfter(newComma, lastArrayValue);
					}

				} else {

					PsiElement lastChild = arrayCreationExpression.getLastChild(); // this is either ')' or ']'
					PsiElement secondLastChild = lastChild.getPrevSibling(); // this is either '('/'[' or whitespace

					PsiElement newWhiteSpace = null;
					PsiElement openingBrace = null; // this is '('/'['
					if (secondLastChild instanceof PsiWhiteSpace) {
						String newSpaceText = secondLastChild.getText();
						if (!newSpaceText.contains(" ") && !newSpaceText.contains("\t") && newSpaceText.contains("\n")) {
							newSpaceText += '\t';
						}
						newWhiteSpace = PsiElementFactory.SERVICE.getInstance(project).createDummyHolder(newSpaceText, PhpTokenTypes.WHITE_SPACE, null);

						openingBrace = secondLastChild.getPrevSibling();
					} else {
						openingBrace = secondLastChild;
					}

					PsiElement newPsi = PsiElementFactory.SERVICE.getInstance(project).createDummyHolder(newValueText, PhpElementTypes.ARRAY_VALUE, null);

					if (newWhiteSpace != null) {

						arrayCreationExpression.addAfter(newPsi, openingBrace);
						arrayCreationExpression.addAfter(newWhiteSpace, openingBrace);

						// this doesn't work, because white space will merge, then secondLastChild no longer valid
//												arrayCreationExpression.addAfter(newWhiteSpace, secondLastChild);
//												arrayCreationExpression.addAfter(newPsi, secondLastChild);

					} else {
						arrayCreationExpression.addAfter(newPsi, secondLastChild);
					}
				}

				List<VirtualFile> files = new ArrayList<VirtualFile>();
				VirtualFile vFile = psiFile.getVirtualFile();
				files.add(vFile);
				PsiDocumentManager.getInstance(project).reparseFiles(files, false);

				// this does work, but style isn't what I want, array values are in same line
//				PsiFile myPsiFile = PsiManager.getInstance(project).findFile(vFile);
//				if (myPsiFile != null) {
//					//must not change document outside command
//					CodeStyleManager.getInstance(project).reformat(myPsiFile);
//				}

//				int i = 0;

			}
		}, null, null, doc);

	}

	public static void addModuleToAppConfig(@NotNull Project project, @NotNull String moduleName) {
		VirtualFile baseDir = project.getBaseDir();
		VirtualFile appConfigFile = baseDir.findFileByRelativePath("config/application.config.php");
		if (appConfigFile == null || appConfigFile.isDirectory()) {
			return;
		}

		PsiFile appConfigPsiFile = PsiManager.getInstance(project).findFile(appConfigFile);
		if (appConfigPsiFile == null) {
			return;
		}

		PsiElement[] children = appConfigPsiFile.getChildren();
		for (PsiElement child : children) {
			if (child instanceof GroupStatement) {
				GroupStatement groupStatement = (GroupStatement)child;

				for (PsiElement statementChild : groupStatement.getStatements()) {
					if (statementChild instanceof PhpReturn) {
						PhpReturn phpReturn = (PhpReturn)statementChild;
						if (phpReturn.getArgument() instanceof ArrayCreationExpression) {
							ArrayCreationExpression topArrayCreation = (ArrayCreationExpression)phpReturn.getArgument();

							ArrayHashElement modulesHash = findArrayHashWithKey(topArrayCreation, "modules");
							if (modulesHash != null &&
									modulesHash.getValue() != null &&
									modulesHash.getValue() instanceof ArrayCreationExpression) {

								ArrayCreationExpression modulesArray = (ArrayCreationExpression)modulesHash.getValue();

								addArrayValueToArrayCreation(modulesArray, "\'" + moduleName + "\'");

								return;
							}
						}
						break;
					}
				}

			}

		}
	}

	private static ArrayHashElement findArrayHashWithKey(@NotNull ArrayCreationExpression array, @NotNull String key) {
		for (ArrayHashElement arrayHash : array.getHashElements()) {
			if (arrayHash.getKey() != null && arrayHash.getKey()instanceof StringLiteralExpression) {
				StringLiteralExpression keyLiteral = (StringLiteralExpression)arrayHash.getKey();
				if (keyLiteral.getContents().equals(key)) {
					return arrayHash;
				}
			}
		}
		return null;
	}



	public static boolean isValidModuleName(@NotNull String moduleName) {
		if (Pattern.matches("^[_a-zA-Z][_a-zA-Z0-9]*$", moduleName)) {
			return true;
		} else {
			return false;
		}
	}

	public static VirtualFile createDirectory(@NotNull Project project, @NotNull VirtualFile baseDir, @NotNull String dirName) {
		try {
			return baseDir.createChildDirectory(project, dirName);
		} catch (IOException e1) {
			//
		}
		return null;
	}

	public static PsiFile createFile(@NotNull Project project, @NotNull VirtualFile baseDir, @NotNull String fileName, @NotNull String fileContent) {
		PsiDirectory basePsiDir = PsiManager.getInstance(project).findDirectory(baseDir);
		if (basePsiDir != null) {

			PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(fileName, FileTypes.PLAIN_TEXT, fileContent);

			basePsiDir.add(psiFile);

			return psiFile;
		}
		return null;
	}

	public static String readResourceFileText(@NotNull String resourcePath) {
		String content;
		try {
			content = StreamUtil.convertSeparators(StreamUtil.readText(ZF2Util.class.getResourceAsStream(resourcePath), "UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return content;
	}
}
