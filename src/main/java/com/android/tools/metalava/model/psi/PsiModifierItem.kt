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

import com.android.tools.metalava.model.AnnotationItem
import com.android.tools.metalava.model.Codebase
import com.android.tools.metalava.model.DefaultModifierList
import com.android.tools.metalava.model.ModifierList
import com.android.tools.metalava.model.MutableModifierList
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.kotlin.asJava.elements.KtLightModifierList
import org.jetbrains.kotlin.lexer.KtTokens

class PsiModifierItem(
    codebase: Codebase,
    flags: Int = 0,
    annotations: MutableList<AnnotationItem>? = null
) : DefaultModifierList(codebase, flags, annotations), ModifierList, MutableModifierList {
    companion object {
        fun create(codebase: PsiBasedCodebase, element: PsiModifierListOwner, documentation: String?): PsiModifierItem {
            val modifiers = create(
                codebase,
                element.modifierList
            )

            if (documentation?.contains("@deprecated") == true ||
                // Check for @Deprecated annotation
                ((element as? PsiDocCommentOwner)?.isDeprecated == true)
            ) {
                modifiers.setDeprecated(true)
            }

            return modifiers
        }

        fun create(codebase: PsiBasedCodebase, modifierList: PsiModifierList?): PsiModifierItem {
            modifierList ?: return PsiModifierItem(codebase)

            var flags = 0
            if (modifierList.hasModifierProperty(PsiModifier.PUBLIC)) {
                flags = flags or PUBLIC
            }
            if (modifierList.hasModifierProperty(PsiModifier.PROTECTED)) {
                flags = flags or PROTECTED
            }
            if (modifierList.hasModifierProperty(PsiModifier.PRIVATE)) {
                flags = flags or PRIVATE
            }
            if (modifierList.hasModifierProperty(PsiModifier.STATIC)) {
                flags = flags or STATIC
            }
            if (modifierList.hasModifierProperty(PsiModifier.ABSTRACT)) {
                flags = flags or ABSTRACT
            }
            if (modifierList.hasModifierProperty(PsiModifier.FINAL)) {
                flags = flags or FINAL
            }
            if (modifierList.hasModifierProperty(PsiModifier.NATIVE)) {
                flags = flags or NATIVE
            }
            if (modifierList.hasModifierProperty(PsiModifier.SYNCHRONIZED)) {
                flags = flags or SYNCHRONIZED
            }
            if (modifierList.hasModifierProperty(PsiModifier.STRICTFP)) {
                flags = flags or STRICT_FP
            }
            if (modifierList.hasModifierProperty(PsiModifier.TRANSIENT)) {
                flags = flags or TRANSIENT
            }
            if (modifierList.hasModifierProperty(PsiModifier.VOLATILE)) {
                flags = flags or VOLATILE
            }
            if (modifierList.hasModifierProperty(PsiModifier.DEFAULT)) {
                flags = flags or DEFAULT
            }

            // Look for special Kotlin keywords
            if (modifierList is KtLightModifierList<*>) {
                val ktModifierList = modifierList.kotlinOrigin
                if (ktModifierList != null) {
                    if (ktModifierList.hasModifier(KtTokens.VARARG_KEYWORD)) {
                        flags = flags or VARARG
                    }
                    if (ktModifierList.hasModifier(KtTokens.SEALED_KEYWORD)) {
                        flags = flags or SEALED
                    }
                    if (ktModifierList.hasModifier(KtTokens.INTERNAL_KEYWORD)) {
                        // Also remove public flag which at the UAST levels it promotes these
                        // methods to, e.g. "internal myVar" gets turned into
                        //    public final boolean getMyHiddenVar$lintWithKotlin()
                        flags = (flags or INTERNAL) and PUBLIC.inv()
                    }
                    if (ktModifierList.hasModifier(KtTokens.INFIX_KEYWORD)) {
                        flags = flags or INFIX
                    }
                    if (ktModifierList.hasModifier(KtTokens.OPERATOR_KEYWORD)) {
                        flags = flags or OPERATOR
                    }
                    if (ktModifierList.hasModifier(KtTokens.INLINE_KEYWORD)) {
                        flags = flags or INLINE
                    }
                }
            }

            val psiAnnotations = modifierList.annotations
            return if (psiAnnotations.isEmpty()) {
                PsiModifierItem(codebase, flags)
            } else {
                val annotations: MutableList<AnnotationItem> =
                    psiAnnotations.map { PsiAnnotationItem.create(codebase, it) }.toMutableList()
                PsiModifierItem(codebase, flags, annotations)
            }
        }

        fun create(codebase: PsiBasedCodebase, original: PsiModifierItem): PsiModifierItem {
            val originalAnnotations = original.annotations ?: return PsiModifierItem(codebase, original.flags)
            val copy: MutableList<AnnotationItem> = ArrayList(originalAnnotations.size)
            originalAnnotations.mapTo(copy) { PsiAnnotationItem.create(codebase, it as PsiAnnotationItem) }
            return PsiModifierItem(codebase, original.flags, copy)
        }
    }
}
