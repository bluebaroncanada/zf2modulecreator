package org.anathan.zf2modulecreator;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.navigation.ItemPresentation;
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
								PhpPsiElement firstPsiChild = modulesArray.getFirstPsiChild();
								PhpPsiElement currentPsiChild = firstPsiChild;
								PhpPsiElement lastPsiChild = null;

								while (currentPsiChild != null) {
									if (currentPsiChild.getFirstPsiChild() != null &&
											currentPsiChild.getFirstPsiChild() instanceof StringLiteralExpression) {
										StringLiteralExpression literal = (StringLiteralExpression)currentPsiChild.getFirstPsiChild();
										if (literal.getContents().equals(moduleName)) {
											return;
										}

									}

									if (currentPsiChild.getNextPsiSibling() != null) {
										lastPsiChild = currentPsiChild;
									}
									currentPsiChild = currentPsiChild.getNextPsiSibling();
								}

//								PhpPsiElement newArrayValue = PhpPsiElementFactory.createExpressionCodeFragment(project, '\'' + moduleName + '\'', modulesArray, true);
//								PhpPsiElement newStringLiteral = PhpPsiElementFactory.createPhpPsiFromText(project, PhpTokenTypes.STRING_LITERAL_SINGLE_QUOTE, '\'' + moduleName + '\'');

//								newArrayValue.add(newStringLiteral);
//								modulesArray.getNode().addChild(newArrayValue.getNode());
//								modulesArray.add(newStringLiteral);

								lastPsiChild = (PhpPsiElement)modulesArray.getLastChild();
								if (lastPsiChild == null) {
//									modulesArray.addBefore(newArrayValue, modulesArray.getLastChild());

//									CodeStyleManager.getInstance(project).reformat(modulesArray);

									Document doc = PsiDocumentManager.getInstance(project).getDocument(modulesArray.getContainingFile());
									if (doc != null) {
										String modulesArrayText = doc.getText(modulesArray.getTextRange());

										int lastBraceIndex = modulesArrayText.lastIndexOf(')');
										if (lastBraceIndex >= 0) {
											int lastLeftBraceIndex = modulesArrayText.lastIndexOf("(", lastBraceIndex);
											int lastLeftBraceEnd = lastLeftBraceIndex + 1;
											String insertedText = "    " + "    " + "\'" + moduleName + "\'" + "\r\n";
											String convertedText = StreamUtil.convertSeparators(insertedText);
											doc.insertString(lastLeftBraceEnd, convertedText);
										}

										PsiDocumentManager.getInstance(project).commitDocument(doc);
									}

								} else {
//									PsiElement endBrace = modulesArray.getLastChild();
//									PsiElement[] children1 = modulesArray.getChildren();
//
//
//									PsiElement currentSibling = null;
//
//									PsiElement lastComma = null;
//									while (currentSibling != null) {
//										if (currentSibling == lastPsiChild) {
//											lastComma = null;
//											break;
//										} else {
//											if (currentSibling.getText().equals(",")) {
//												lastComma = currentSibling;
//												break;
//											}
//										}
//
//										currentSibling = currentSibling.getPrevSibling();
//									}
//
//									if (lastComma == null) {
//										PsiElement comma = PhpPsiElementFactory.createComma(project);
//										modulesArray.addAfter(comma, lastPsiChild);
//										modulesArray.addBefore(newArrayValue, modulesArray.getLastChild());
//
////										CodeStyleManager.getInstance(project).reformat(modulesArray);
//									} else {
//										modulesArray.addBefore(newArrayValue, modulesArray.getLastChild());
//									}


									Document doc = PsiDocumentManager.getInstance(project).getDocument(modulesArray.getContainingFile());
									if (doc != null) {
										String modulesArrayText = doc.getText(modulesArray.getTextRange());

										int lastBraceIndex = modulesArrayText.lastIndexOf(')');
										if (lastBraceIndex >= 0) {
											int lastPsiChildIndex = modulesArrayText.lastIndexOf(lastPsiChild.getText(), lastBraceIndex);
											int lastPsiChildEnd = lastPsiChildIndex + lastPsiChild.getText().length();
											String insertedText = "    " + "    " + "\'" + moduleName + "\'" + "\r\n";
											String convertedText = StreamUtil.convertSeparators(insertedText);
											doc.insertString(lastPsiChildEnd, convertedText);
										}

										PsiDocumentManager.getInstance(project).commitDocument(doc);
									}
								}

//								CodeStyleManager.getInstance(project).reformat(appConfigPsiFile);
//								PsiManager.getInstance(project).reloadFromDisk(appConfigPsiFile);

//								PsiManager.getInstance(project).
//								if (newArrayValue.getPrevSibling() != null) {
//									CodeStyleManager.getInstance(project).reformatText(appConfigPsiFile, newArrayValue.getPrevSibling().getTextRange().getEndOffset(), newArrayValue.getTextRange().getEndOffset());
//								} else {
//									CodeStyleManager.getInstance(project).reformat(appConfigPsiFile, modulesArray.getTextRange().getStartOffset(), newArrayValue.getTextRange().getEndOffset());

//								}


								//.insertString(element.getTextRange().getEndOffset(), ",\n'd'"). You can then call PsiDocumentManager.commitDocument(document)

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
