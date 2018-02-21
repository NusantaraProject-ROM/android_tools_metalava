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

package com.android.tools.metalava.model

import com.android.tools.lint.detector.api.ClassContext
import com.android.tools.metalava.JAVA_LANG_OBJECT
import com.android.tools.metalava.JAVA_LANG_PREFIX
import com.android.tools.metalava.compatibility
import com.android.tools.metalava.options
import java.util.function.Predicate

/** Represents a type */
interface TypeItem {
    /**
     * Generates a string for this type.
     *
     * For a type like this: @Nullable java.util.List<@NonNull java.lang.String>,
     * [outerAnnotations] controls whether the top level annotation like @Nullable
     * is included, [innerAnnotations] controls whether annotations like @NonNull
     * are included, and [erased] controls whether we return the string for
     * the raw type, e.g. just "java.util.List"
     *
     * (The combination [outerAnnotations] = true and [innerAnnotations] = false
     * is not allowed.)
     */
    fun toTypeString(
        outerAnnotations: Boolean = false,
        innerAnnotations: Boolean = outerAnnotations,
        erased: Boolean = false
    ): String

    /** Alias for [toTypeString] with erased=true */
    fun toErasedTypeString(): String

    /** Returns the internal name of the type, as seen in bytecode */
    fun internalName(): String {
        // Default implementation; PSI subclass is more accurate
        return toSlashFormat(toErasedTypeString())
    }

    fun asClass(): ClassItem?

    fun toSimpleType(): String {
        return stripJavaLangPrefix(toTypeString())
    }

    val primitive: Boolean

    fun typeArgumentClasses(): List<ClassItem>

    fun convertType(from: ClassItem, to: ClassItem): TypeItem {
        val map = from.mapTypeVariables(to)
        if (!map.isEmpty()) {
            return convertType(map)
        }

        return this
    }

    fun convertType(replacementMap: Map<String, String>?, owner: Item? = null): TypeItem

    fun convertTypeString(replacementMap: Map<String, String>?): String {
        return convertTypeString(toTypeString(outerAnnotations = true, innerAnnotations = true), replacementMap)
    }

    fun isJavaLangObject(): Boolean {
        return toTypeString() == JAVA_LANG_OBJECT
    }

    fun defaultValue(): Any? {
        return when (toTypeString()) {
            "boolean" -> false
            "byte" -> 0.toByte()
            "char" -> '\u0000'
            "short" -> 0.toShort()
            "int" -> 0
            "long" -> 0L
            "float" -> 0f
            "double" -> 0.0
            else -> null
        }
    }

    /** Returns true if this type references a type not matched by the given predicate */
    fun referencesExcludedType(filter: Predicate<Item>): Boolean {
        if (primitive) {
            return false
        }

        for (item in typeArgumentClasses()) {
            if (!filter.test(item)) {
                return true
            }
        }

        return false
    }

    fun defaultValueString(): String = defaultValue()?.toString() ?: "null"

    fun hasTypeArguments(): Boolean = toTypeString().contains("<")

    fun isTypeParameter(): Boolean = toTypeString().length == 1 // heuristic; accurate implementation in PSI subclass

    companion object {
        /** Shortens types, if configured */
        fun shortenTypes(type: String): String {
            if (options.omitCommonPackages) {
                var cleaned = type
                if (cleaned.contains("@android.support.annotation.")) {
                    cleaned = cleaned.replace("@android.support.annotation.", "@")
                }

                return stripJavaLangPrefix(cleaned)
            }

            return type
        }

        /**
         * Removes java.lang. prefixes from types, unless it's in a subpackage such
         * as java.lang.reflect
         */
        fun stripJavaLangPrefix(type: String): String {
            if (type.contains(JAVA_LANG_PREFIX)) {
                var cleaned = type

                // Replacing java.lang is harder, since we don't want to operate in sub packages,
                // e.g. java.lang.String -> String, but java.lang.reflect.Method -> unchanged
                var index = cleaned.indexOf(JAVA_LANG_PREFIX)
                while (index != -1) {
                    val start = index + JAVA_LANG_PREFIX.length
                    val end = cleaned.length
                    for (index2 in start..end) {
                        if (index2 == end) {
                            val suffix = cleaned.substring(start)
                            cleaned = if (index == 0) {
                                suffix
                            } else {
                                cleaned.substring(0, index) + suffix
                            }
                            break
                        }
                        val c = cleaned[index2]
                        if (c == '.') {
                            break
                        } else if (!Character.isJavaIdentifierPart(c)) {
                            val suffix = cleaned.substring(start)
                            cleaned = if (index == 0) {
                                suffix
                            } else {
                                cleaned.substring(0, index) + suffix
                            }
                            break
                        }
                    }

                    index = cleaned.indexOf(JAVA_LANG_PREFIX, start)
                }

                return cleaned
            }

            return type
        }

        fun formatType(type: String?): String {
            if (type == null) {
                return ""
            }

            var cleaned = type

            if (compatibility.spacesAfterCommas && cleaned.indexOf(',') != -1) {
                // The compat files have spaces after commas where we normally don't
                cleaned = cleaned.replace(",", ", ").replace(",  ", ", ")
            }

            cleaned = cleanupGenerics(cleaned)
            return cleaned
        }

        fun cleanupGenerics(signature: String): String {
            // <T extends java.lang.Object> is the same as <T>
            //  but NOT for <T extends Object & java.lang.Comparable> -- you can't
            //  shorten this to <T & java.lang.Comparable
            //return type.replace(" extends java.lang.Object", "")
            return signature.replace(" extends java.lang.Object>", ">")

        }

        val comparator: Comparator<TypeItem> = Comparator { type1, type2 ->
            val cls1 = type1.asClass()
            val cls2 = type2.asClass()
            if (cls1 != null && cls2 != null) {
                ClassItem.fullNameComparator.compare(cls1, cls2)
            } else {
                type1.toTypeString().compareTo(type2.toTypeString())
            }
        }

        fun convertTypeString(typeString: String, replacementMap: Map<String, String>?): String {
            var string = typeString
            if (replacementMap != null && replacementMap.isNotEmpty()) {
                // This is a moved method (typically an implementation of an interface
                // method provided in a hidden superclass), with generics signatures.
                // We need to rewrite the generics variables in case they differ
                // between the classes.
                if (!replacementMap.isEmpty()) {
                    replacementMap.forEach { from, to ->
                        // We can't just replace one string at a time:
                        // what if I have a map of {"A"->"B", "B"->"C"} and I tried to convert A,B,C?
                        // If I do the replacements one letter at a time I end up with C,C,C; if I do the substitutions
                        // simultaneously I get B,C,C. Therefore, we insert "___" as a magical prefix to prevent
                        // scenarios like this, and then we'll drop them afterwards.
                        string = string.replace(Regex(pattern = """\b$from\b"""), replacement = "___$to")
                    }
                }
                string = string.replace("___", "")
                return string
            } else {
                return string
            }
        }

        // Copied from doclava1
        fun toSlashFormat(typeName: String): String {
            var name = typeName
            var dimension = ""
            while (name.endsWith("[]")) {
                dimension += "["
                name = name.substring(0, name.length - 2)
            }

            val base: String
            if (name == "void") {
                base = "V"
            } else if (name == "byte") {
                base = "B"
            } else if (name == "boolean") {
                base = "Z"
            } else if (name == "char") {
                base = "C"
            } else if (name == "short") {
                base = "S"
            } else if (name == "int") {
                base = "I"
            } else if (name == "long") {
                base = "L"
            } else if (name == "float") {
                base = "F"
            } else if (name == "double") {
                base = "D"
            } else {
                base = "L" + ClassContext.getInternalName(name) + ";"
            }

            return dimension + base
        }
    }
}