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

package com.android.tools.metalava

import com.android.SdkConstants.ATTR_VALUE
import com.android.tools.metalava.Severity.ERROR
import com.android.tools.metalava.Severity.HIDDEN
import com.android.tools.metalava.Severity.INHERIT
import com.android.tools.metalava.Severity.LINT
import com.android.tools.metalava.Severity.WARNING
import com.android.tools.metalava.doclava1.Errors
import com.android.tools.metalava.model.AnnotationArrayAttributeValue
import com.android.tools.metalava.model.Item
import com.android.tools.metalava.model.configuration
import com.android.tools.metalava.model.psi.PsiItem
import com.android.tools.metalava.model.text.TextItem
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.impl.light.LightElement
import java.io.File

var reporter = Reporter()

enum class Severity(private val displayName: String) {
    INHERIT("inherit"),

    HIDDEN("hidden"),

    /**
     * Lint level means that we encountered inconsistent or broken documentation.
     * These should be resolved, but don't impact API compatibility.
     */
    LINT("lint"),

    /**
     * Warning level means that we encountered some incompatible or inconsistent
     * API change. These must be resolved to preserve API compatibility.
     */
    WARNING("warning"),

    /**
     * Error level means that we encountered severe trouble and were unable to
     * output the requested documentation.
     */
    ERROR("error");

    override fun toString(): String = displayName
}

open class Reporter(private val rootFolder: File? = null) {
    private var hasErrors = false

    fun error(item: Item?, message: String, id: Errors.Error? = null): Boolean {
        return error(item?.psi(), message, id)
    }

    fun warning(item: Item?, message: String, id: Errors.Error? = null): Boolean {
        return warning(item?.psi(), message, id)
    }

    fun error(element: PsiElement?, message: String, id: Errors.Error? = null): Boolean {
        // Using lowercase since that's the convention doclava1 is using
        return report(ERROR, element, message, id)
    }

    fun warning(element: PsiElement?, message: String, id: Errors.Error? = null): Boolean {
        return report(WARNING, element, message, id)
    }

    fun report(id: Errors.Error, element: PsiElement?, message: String): Boolean {
        return report(configuration.getSeverity(id), element, message, id)
    }

    fun report(id: Errors.Error, file: File?, message: String): Boolean {
        return report(configuration.getSeverity(id), file?.path, message, id)
    }

    fun report(id: Errors.Error, item: Item?, message: String): Boolean {
        if (isSuppressed(id, item, message)) {
            return false
        }

        val severity = configuration.getSeverity(id)
        return when (item) {
            is PsiItem -> {
                report(severity, item.psi(), message, id)
            }
            is TextItem -> report(severity, (item as? TextItem)?.position.toString(), message, id)
            else -> report(severity, "<unknown location>", message, id)
        }
    }

    fun isSuppressed(id: Errors.Error, item: Item? = null, message: String? = null): Boolean {
        val severity = configuration.getSeverity(id)
        if (severity == HIDDEN) {
            return true
        }

        item ?: return false

        if (severity == LINT || severity == WARNING || severity == ERROR) {
            val annotation = item.modifiers.findAnnotation("android.annotation.SuppressLint")
            if (annotation != null) {
                val attribute = annotation.findAttribute(ATTR_VALUE)
                if (attribute != null) {
                    val id1 = "Doclava${id.code}"
                    val id2 = id.name
                    val value = attribute.value
                    if (value is AnnotationArrayAttributeValue) {
                        // Example: @SuppressLint({"DocLava1", "DocLava2"})
                        for (innerValue in value.values) {
                            val string = innerValue.value()?.toString() ?: continue
                            if (suppressMatches(string, id1, message) || suppressMatches(string, id2, message)) {
                                return true
                            }
                        }
                    } else {
                        // Example: @SuppressLint("DocLava1")
                        val string = value.value()?.toString()
                        if (string != null && (
                                suppressMatches(string, id1, message) || suppressMatches(string, id2, message))
                        ) {
                            return true
                        }
                    }
                }
            }
        }

        return false
    }

    private fun suppressMatches(value: String, id: String?, message: String?): Boolean {
        id ?: return false

        if (value == id) {
            return true
        }

        if (message != null && value.startsWith(id) && value.endsWith(message) &&
            (value == "$id:$message" || value == "$id: $message")
        ) {
            return true
        }

        return false
    }

    private fun getTextRange(element: PsiElement): TextRange? {
        var range: TextRange? = null

        if (element is PsiCompiledElement) {
            if (element is LightElement) {
                range = (element as PsiElement).textRange
            }
            if (range == null || TextRange.EMPTY_RANGE == range) {
                return null
            }
        } else {
            range = element.textRange
        }

        return range
    }

    fun elementToLocation(element: PsiElement?, includeDocs: Boolean = true): String? {
        element ?: return null
        val psiFile = element.containingFile ?: return null
        val virtualFile = psiFile.virtualFile ?: return null
        val file = VfsUtilCore.virtualToIoFile(virtualFile)

        val path =
            if (rootFolder != null) {
                val root: VirtualFile? = StandardFileSystems.local().findFileByPath(rootFolder.path)
                if (root != null) VfsUtilCore.getRelativePath(virtualFile, root) ?: file.path else file.path
            } else {
                file.path
            }

        // Skip doc comments for classes and methods; we usually want to point right to
        // the class/method definition
        val rangeElement = if (!includeDocs && element is PsiModifierListOwner) {
            element.modifierList ?: element
        } else
            element

        val range = getTextRange(rangeElement)
        return if (range == null) {
            // No source offsets, just use filename
            path
        } else {
            val lineNumber = getLineNumber(psiFile.text, range.startOffset) + 1
            "$path:$lineNumber"
        }
    }

    /** Returns the 0-based line number */
    private fun getLineNumber(text: String, offset: Int): Int {
        var line = 0
        var curr = 0
        val target = Math.min(offset, text.length)
        while (curr < target) {
            if (text[curr++] == '\n') {
                line++
            }
        }
        return line
    }

    open fun report(severity: Severity, element: PsiElement?, message: String, id: Errors.Error? = null): Boolean {
        if (severity == HIDDEN) {
            return false
        }

        return report(severity, elementToLocation(element), message, id)
    }

    open fun report(
        severity: Severity,
        location: String?,
        message: String,
        id: Errors.Error? = null,
        color: Boolean = options.color
    ): Boolean {
        if (severity == HIDDEN) {
            return false
        }

        val effectiveSeverity =
            if (severity == LINT && options.lintsAreErrors)
                ERROR
            else if (severity == WARNING && options.warningsAreErrors) {
                ERROR
            } else {
                severity
            }

        if (severity == ERROR) {
            hasErrors = true
        }

        val sb = StringBuilder(100)

        if (color) {
            sb.append(terminalAttributes(bold = true))
            if (!options.omitLocations) {
                location?.let { sb.append(it).append(": ") }
            }
            when (effectiveSeverity) {
                LINT -> sb.append(terminalAttributes(foreground = TerminalColor.CYAN)).append("lint: ")
                WARNING -> sb.append(terminalAttributes(foreground = TerminalColor.YELLOW)).append("warning: ")
                ERROR -> sb.append(terminalAttributes(foreground = TerminalColor.RED)).append("error: ")
                INHERIT, HIDDEN -> {
                }
            }
            sb.append(resetTerminal())
            sb.append(message)
            id?.let { sb.append(" [").append(if (it.name != null) it.name else it.code).append("]") }
        } else {
            if (!options.omitLocations) {
                location?.let { sb.append(it).append(": ") }
            }
            if (compatibility.oldErrorOutputFormat) {
                // according to doclava1 there are some people or tools parsing old format
                when (effectiveSeverity) {
                    LINT -> sb.append("lint ")
                    WARNING -> sb.append("warning ")
                    ERROR -> sb.append("error ")
                    INHERIT, HIDDEN -> {
                    }
                }
                id?.let { sb.append(if (it.name != null) it.name else it.code).append(": ") }
                sb.append(message)
            } else {
                when (effectiveSeverity) {
                    LINT -> sb.append("lint: ")
                    WARNING -> sb.append("warning: ")
                    ERROR -> sb.append("error: ")
                    INHERIT, HIDDEN -> {
                    }
                }
                sb.append(message)
                id?.let {
                    sb.append(" [")
                    if (it.name != null) {
                        sb.append(it.name).append(":")
                    }
                    sb.append(it.code)
                    sb.append("]")
                }
            }
        }
        print(sb.toString())
        return true
    }

    open fun print(message: String) {
        options.stdout.println()
        options.stdout.print(message.trim())
        options.stdout.flush()
    }

    fun hasErrors(): Boolean = hasErrors
}