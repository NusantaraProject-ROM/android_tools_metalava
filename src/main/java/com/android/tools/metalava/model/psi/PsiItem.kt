/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.metalava.model.psi

import com.android.tools.metalava.model.DefaultItem
import com.android.tools.metalava.model.MutableModifierList
import com.android.tools.metalava.model.ParameterItem
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.uast.UElement
import org.jetbrains.uast.sourcePsiElement

abstract class PsiItem(
    override val codebase: PsiBasedCodebase,
    val element: PsiElement,
    override val modifiers: PsiModifierItem,
    override var documentation: String
) : DefaultItem() {

    override var deprecated: Boolean = modifiers.isDeprecated()

    @Suppress("LeakingThis") // Documentation can change, but we don't want to pick up subsequent @docOnly mutations
    override var docOnly = documentation.contains("@doconly")
    @Suppress("LeakingThis")
    override var removed = documentation.contains("@removed")

    @Suppress("LeakingThis")
    override var originallyHidden =
        documentation.contains('@') &&
            (documentation.contains("@hide") ||
                documentation.contains("@pending") ||
                // KDoc:
                documentation.contains("@suppress")) ||
            modifiers.hasHideAnnotations()

    @Suppress("LeakingThis")
    override var hidden = originallyHidden && !modifiers.hasShowAnnotation()

    override fun psi(): PsiElement? = element

    // TODO: Consider only doing this in tests!
    override fun isFromClassPath(): Boolean {
        return if (element is UElement) {
            (element.sourcePsi ?: element.javaPsi) is PsiCompiledElement
        } else {
            element is PsiCompiledElement
        }
    }

    override fun isCloned(): Boolean = false

    /** Get a mutable version of modifiers for this item */
    override fun mutableModifiers(): MutableModifierList = modifiers

    override fun findTagDocumentation(tag: String): String? {
        if (element is PsiCompiledElement) {
            return null
        }
        if (documentation.isBlank()) {
            return null
        }

        // We can't just use element.docComment here because we may have modified
        // the comment and then the comment snapshot in PSI isn't up to date with our
        // latest changes
        val docComment = codebase.getComment(documentation)
        val docTag = docComment.findTagByName(tag) ?: return null
        val text = docTag.text

        // Trim trailing next line (javadoc *)
        var index = text.length - 1
        while (index > 0) {
            val c = text[index]
            if (!(c == '*' || c.isWhitespace())) {
                break
            }
            index--
        }
        index++
        return if (index < text.length) {
            text.substring(0, index)
        } else {
            text
        }
    }

    override fun appendDocumentation(comment: String, tagSection: String?, append: Boolean) {
        if (comment.isBlank()) {
            return
        }

        // TODO: Figure out if an annotation should go on the return value, or on the method.
        // For example; threading: on the method, range: on the return value.
        // TODO: Find a good way to add or append to a given tag (@param <something>, @return, etc)

        if (this is ParameterItem) {
            // For parameters, the documentation goes into the surrounding method's documentation!
            // Find the right parameter location!
            val parameterName = name()
            val target = containingMethod()
            target.appendDocumentation(comment, parameterName)
            return
        }

        documentation = mergeDocumentation(documentation, element, comment.trim(), tagSection, append)
    }

    override fun fullyQualifiedDocumentation(): String {
        return fullyQualifiedDocumentation(documentation)
    }

    override fun fullyQualifiedDocumentation(documentation: String): String {
        return toFullyQualifiedDocumentation(this, documentation)
    }

    /** Finish initialization of the item */
    open fun finishInitialization() {
        modifiers.setOwner(this)
    }

    override fun isKotlin(): Boolean {
        return isKotlin(element)
    }

    companion object {
        fun javadoc(element: PsiElement): String {
            if (element is PsiCompiledElement) {
                return ""
            }

            if (element is UElement) {
                val comments = element.comments
                if (comments.isNotEmpty()) {
                    val sb = StringBuilder()
                    comments.asSequence().joinTo(buffer = sb, separator = "\n")
                    return sb.toString()
                } else {
                    // Temporary workaround: UAST seems to not return document nodes
                    // https://youtrack.jetbrains.com/issue/KT-22135
                    val first = element.sourcePsiElement?.firstChild
                    if (first is KDoc) {
                        return first.text
                    }
                }
            }

            if (element is PsiDocCommentOwner && element.docComment !is PsiCompiledElement) {
                return element.docComment?.text ?: ""
            }

            return ""
        }

        fun modifiers(
            codebase: PsiBasedCodebase,
            element: PsiModifierListOwner,
            documentation: String
        ): PsiModifierItem {
            return PsiModifierItem.create(codebase, element, documentation)
        }

        fun isKotlin(element: PsiElement): Boolean {
            return element.language === KotlinLanguage.INSTANCE
        }
    }
}
