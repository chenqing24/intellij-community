// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.idea.devkit.actions

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.ui.components.CheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.*
import org.jetbrains.idea.devkit.DevKitBundle
import org.jetbrains.idea.devkit.util.PsiUtil
import java.util.*
import javax.swing.JComponent

/**
 * @author Konstantin Bulenkov
 */
class NewThemeAction: AnAction() {
  val THEME_JSON_TEMPLATE = "ThemeJson.json"

  @Suppress("UsePropertyAccessSyntax") // IdeView#getOrChooseDirectory is not a getter
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(LangDataKeys.IDE_VIEW) ?: return
    val dir = view.getOrChooseDirectory() ?: return

    val project = e.getRequiredData(LangDataKeys.PROJECT)
    val dialog = NewThemeDialog(project)
    dialog.show()

    if (dialog.exitCode == DialogWrapper.OK_EXIT_CODE) {
      val file = createThemeJson(dialog.name.text, dialog.isDark.isSelected, project, dir)
      registerTheme(file)
    }
  }

  override fun update(e: AnActionEvent) {
    val module = e.getData(LangDataKeys.MODULE)
    e.presentation.isEnabled = module != null && PsiUtil.isPluginModule(module)
  }

  private fun createThemeJson(themeName: String, isDark: Boolean, project: Project, dir: PsiDirectory): PsiFile {
    val fileName = getThemeJsonFileName(themeName)
    val template = FileTemplateManager.getInstance(project).getJ2eeTemplate(THEME_JSON_TEMPLATE)
    val props = Properties()
    props.setProperty("NAME", themeName) //TODO escape for JSON
    props.setProperty("IS_DARK", isDark.toString())

    val created = FileTemplateUtil.createFromTemplate(template, fileName, props, dir)
    assert(created is PsiFile)
    return created as PsiFile
  }

  private fun getThemeJsonFileName(themeName: String): String {
    return FileUtil.sanitizeFileName(themeName) + ".theme.json"
  }

  private fun registerTheme(file: PsiFile) {
    //TODO implement
  }


  class NewThemeDialog(project: Project) : DialogWrapper(project) {
    val name = JBTextField()
    val isDark = CheckBox(DevKitBundle.message("new.theme.dialog.is.dark.checkbox.text"), true)

    init {
      title = DevKitBundle.message("new.theme.dialog.title")
      init()
    }

    override fun createCenterPanel(): JComponent? {
      return panel {
        row(DevKitBundle.message("new.theme.dialog.name.text.field.text")) {
          cell {name(growPolicy = GrowPolicy.MEDIUM_TEXT)}
        }
        row("") {
          cell { isDark() }
        }
      }
    }

    override fun getPreferredFocusedComponent(): JComponent? {
      return name
    }

    override fun doValidate(): ValidationInfo? {
      if (name.text.isBlank()) return ValidationInfo(DevKitBundle.message("new.theme.dialog.name.empty"), name)
      //TODO max name length, maybe some other restrictions?
      return null
    }
  }
}
