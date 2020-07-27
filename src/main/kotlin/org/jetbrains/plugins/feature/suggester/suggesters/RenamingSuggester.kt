package org.jetbrains.plugins.feature.suggester.suggesters

import com.intellij.openapi.command.CommandProcessor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.plugins.feature.suggester.FeatureSuggester
import org.jetbrains.plugins.feature.suggester.NoSuggestion
import org.jetbrains.plugins.feature.suggester.Suggestion
import org.jetbrains.plugins.feature.suggester.actions.BeforeChildReplacedAction
import org.jetbrains.plugins.feature.suggester.actions.ChildReplacedAction
import org.jetbrains.plugins.feature.suggester.history.UserActionsHistory
import java.util.concurrent.TimeUnit

class RenamingSuggester : FeatureSuggester {
    companion object {
        const val POPUP_MESSAGE = "Why not to use safe renaming: Shift + F6?"
        const val SUGGESTING_ACTION_ID = "Rename"
        const val SUGGESTING_TIP_FILENAME = "neue-Rename.html"
        const val NUMBER_OF_RENAMES_TO_GET_SUGGESTION = 3
        const val MIN_NOTIFICATION_INTERVAL_DAYS = 14
    }

    private val actionsSummary = actionsLocalSummary()

    private class RenamedIdentifiersData(val initialState: String, val references: List<PsiElement>) {
        fun isAllRenamed(): Boolean {
            if (references.size < NUMBER_OF_RENAMES_TO_GET_SUGGESTION) return false
            val firstName = references[0].getIdentifiersName()
            return !(firstName == initialState || references.any { it.getIdentifiersName() != firstName })
        }

        private fun PsiElement.getIdentifiersName(): String {
            val namedElement = this as? PsiNamedElement
            return if (namedElement != null && namedElement.name != null) {
                namedElement.name!!.truncateQualifier()
            } else {
                text.truncateQualifier()
            }
        }

        private fun String.truncateQualifier(): String {
            return takeLastWhile { it != '.' }
        }
    }

    private var renamedIdentifiersData = RenamedIdentifiersData("", emptyList())

    override fun getSuggestion(actions: UserActionsHistory): Suggestion {
        val name = CommandProcessor.getInstance().currentCommandName
        if (name != null && name != "Paste") {
            return NoSuggestion
        }
        when (val lastAction = actions.lastOrNull()) {
            is BeforeChildReplacedAction -> {
                val (parent, newChild, oldChild) = lastAction
                if (parent == null || newChild == null || oldChild == null) return NoSuggestion
                if (oldChild.isIdentifier()) {
                    if (!renamedIdentifiersData.references.contains(parent)) {
                        // TODO Find out why resolve reference causes:
                        //  "java.lang.Throwable: Somebody has requested stubbed spine during PSI operations; not only is this expensive, but will also cause stub PSI invalidation"
                        //  Can be reproduced placing '{' before another code block "{ ... }"
                        val declaration = parent.resolveRef() ?: parent
                        val references = arrayListOf(declaration, *declaration.getAllReferences().toTypedArray())
                        renamedIdentifiersData = RenamedIdentifiersData(oldChild.text, references)
                    }
                }
            }
            is ChildReplacedAction -> {
                val (parent, newChild, oldChild) = lastAction
                if (parent == null || newChild == null || oldChild == null) return NoSuggestion
                if (newChild.isIdentifier()) {
                    if (renamedIdentifiersData.references.contains(parent)
                        && renamedIdentifiersData.isAllRenamed()
                    ) {
                        return createSuggestion(
                            null,
                            POPUP_MESSAGE,
                            suggestingActionDisplayName,
                            SUGGESTING_TIP_FILENAME
                        )
                    }
                }
            }
        }
        return NoSuggestion
    }

    override fun isSuggestionNeeded(): Boolean {
        return super.isSuggestionNeeded(
            actionsSummary,
            SUGGESTING_ACTION_ID,
            TimeUnit.DAYS.toMillis(MIN_NOTIFICATION_INTERVAL_DAYS.toLong())
        )
    }

    private fun PsiElement.getAllReferences(): List<PsiElement> {
        return ReferencesSearch.search(this).map(PsiReference::getElement)
    }

    override val suggestingActionDisplayName: String = "Rename all occurrences"
}