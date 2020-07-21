package org.jetbrains.plugins.feature.suggester

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.keymap.KeymapUtil
import org.jetbrains.plugins.feature.suggester.history.UserActionsHistory

interface FeatureSuggester {

    companion object {
        val EP_NAME: ExtensionPointName<FeatureSuggester> =
            ExtensionPointName.create("org.intellij.featureSuggester.featureSuggester")

        val suggesters: List<FeatureSuggester> = EP_NAME.extensionList

        fun createMessageWithShortcut(actionId: String, suggestionMessage: String): String {
            val shortcut = KeymapUtil.getShortcutText(actionId)
            return if (shortcut == "<no shortcut>") {
                "$suggestionMessage You can bind this action to convenient shortcut."
            } else {
                "$suggestionMessage $shortcut"
            }
        }
    }

    val needToClearLookup: Boolean
        get() = false

    fun getSuggestion(actions: UserActionsHistory): Suggestion

    fun getId(): String
}