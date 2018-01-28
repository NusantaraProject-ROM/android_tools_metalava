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

package com.android.tools.metalava.model.text

import com.android.tools.metalava.model.ClassItem
import com.android.tools.metalava.model.Codebase
import com.android.tools.metalava.model.Item
import com.android.tools.metalava.model.TypeItem

class TextTypeItem(
    val codebase: Codebase,
    val type: String
) : TypeItem {
    override fun toString(): String = type

    override fun toErasedTypeString(): String {
        return toTypeString(false, false, true)
    }

    override fun toTypeString(
        outerAnnotations: Boolean,
        innerAnnotations: Boolean,
        erased: Boolean
    ): String {
        return Companion.toTypeString(type, outerAnnotations, innerAnnotations, erased)
    }

    override fun asClass(): ClassItem? {
        val cls = toErasedTypeString()
        return codebase.findClass(cls)
    }

    fun qualifiedTypeName(): String = type

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextTypeItem) return false

        return qualifiedTypeName() == other.qualifiedTypeName()
    }

    override fun hashCode(): Int {
        return qualifiedTypeName().hashCode()
    }

    override val primitive: Boolean
        get() = isPrimitive(type)

    override fun typeArgumentClasses(): List<ClassItem> = codebase.unsupported()

    override fun convertType(replacementMap: Map<String, String>?, owner: Item?): TypeItem {
        return TextTypeItem(codebase, convertTypeString(replacementMap))
    }

    companion object {
        fun toTypeString(
            type: String,
            outerAnnotations: Boolean,
            innerAnnotations: Boolean,
            erased: Boolean
        ): String {
            return if (erased) {
                val raw = eraseTypeArguments(type)
                if (outerAnnotations && innerAnnotations) {
                    raw
                } else {
                    eraseAnnotations(raw, outerAnnotations, innerAnnotations)
                }
            } else {
                if (outerAnnotations && innerAnnotations) {
                    type
                } else {
                    eraseAnnotations(type, outerAnnotations, innerAnnotations)
                }
            }
        }

        private fun eraseTypeArguments(s: String): String {
            val index = s.indexOf('<')
            if (index != -1) {
                return s.substring(0, index)
            }
            return s
        }

        fun eraseAnnotations(type: String, outer: Boolean, inner: Boolean): String {
            if (type.indexOf('@') == -1) {
                return type
            }

            assert(inner || !outer) // Can't supply outer=true,inner=false

            // Assumption: top level annotations appear first
            val length = type.length
            var max = if (!inner)
                length
            else {
                val space = type.indexOf(' ')
                val generics = type.indexOf('<')
                val first = if (space != -1) {
                    if (generics != -1) {
                        Math.min(space, generics)
                    } else {
                        space
                    }
                } else {
                    generics
                }
                if (first != -1) {
                    first
                } else {
                    length
                }
            }

            var s = type
            while (true) {
                val index = s.indexOf('@')
                if (index == -1 || index >= max) {
                    return s
                }

                // Find end
                val end = findAnnotationEnd(s, index + 1)
                val oldLength = s.length
                s = s.substring(0, index).trim() + s.substring(end).trim()
                val newLength = s.length
                val removed = oldLength - newLength
                max -= removed
            }
        }

        private fun findAnnotationEnd(type: String, start: Int): Int {
            var index = start
            val length = type.length
            var balance = 0
            while (index < length) {
                val c = type[index]
                if (c == '(') {
                    balance++
                } else if (c == ')') {
                    balance--
                    if (balance == 0) {
                        return index + 1
                    }
                } else if (c == '.') {
                } else if (Character.isJavaIdentifierPart(c)) {
                } else if (balance == 0) {
                    break
                }
                index++
            }
            return index
        }

        fun isPrimitive(type: String): Boolean {
            return when (type) {
                "byte", "char", "double", "float", "int", "long", "short", "boolean", "void", "null" -> true
                else -> false
            }
        }
    }
}