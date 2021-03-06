package org.jetbrains.plugins.feature.suggester.suggesters.lang

import com.intellij.psi.PsiElement
import com.intellij.psi.util.findDescendantOfType
import com.jetbrains.python.psi.*
import org.jetbrains.plugins.feature.suggester.suggesters.getParentOfType

class PythonLanguageSupport : LanguageSupport {

    override fun isIfStatement(element: PsiElement): Boolean {
        return element is PyIfStatement
    }

    override fun isForStatement(element: PsiElement): Boolean {
        return element is PyForStatement
    }

    override fun isWhileStatement(element: PsiElement): Boolean {
        return element is PyWhileStatement
    }

    override fun isCodeBlock(element: PsiElement): Boolean {
        return element is PyStatementList
    }

    override fun getCodeBlock(element: PsiElement): PsiElement? {
        return element.findDescendantOfType<PyStatementList>()
    }

    override fun getContainingCodeBlock(element: PsiElement): PsiElement? {
        return element.getParentOfType<PyStatementList>()
    }

    override fun getStatements(element: PsiElement): List<PsiElement> {
        return if (element is PyStatementList) {
            element.statements.toList()
        } else {
            emptyList()
        }
    }

    override fun isSupportedStatementToIntroduceVariable(element: PsiElement): Boolean {
        return element is PyStatement
    }

    override fun isPartOfExpression(element: PsiElement): Boolean {
        return element.getParentOfType<PyExpression>() != null
    }

    override fun isExpressionStatement(element: PsiElement): Boolean {
        return element is PyExpressionStatement
    }

    override fun isVariableDeclaration(element: PsiElement): Boolean {
        return element is PyAssignmentStatement
    }

    override fun getVariableName(element: PsiElement): String? {
        return if (element is PyAssignmentStatement) {
            element.targets.firstOrNull()?.name
        } else {
            null
        }
    }
}