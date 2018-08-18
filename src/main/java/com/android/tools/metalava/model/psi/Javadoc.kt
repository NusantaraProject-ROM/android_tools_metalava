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

import com.intellij.psi.JavaDocTokenType
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.javadoc.PsiDocTag
import com.intellij.psi.javadoc.PsiDocToken
import org.intellij.lang.annotations.Language

/*
 * Various utilities for merging comments into existing javadoc sections.
 *
 * TODO: Handle KDoc
 */

/**
 * Merges the given [newText] into the existing documentation block [existingDoc]
 * (which should be a full documentation node, including the surrounding comment
 * start and end tokens.)
 *
 * If the [tagSection] is null, add the comment to the initial text block
 * of the description. Otherwise if it is "@return", add the comment
 * to the return value. Otherwise the [tagSection] is taken to be the
 * parameter name, and the comment added as parameter documentation
 * for the given parameter.
 */
fun mergeDocumentation(
    existingDoc: String,
    psiElement: PsiElement,
    newText: String,
    tagSection: String?,
    append: Boolean
): String {

    if (existingDoc.isBlank()) {
        // There's no existing comment: Create a new one. This is easy.
        val content = when {
            tagSection == "@return" -> "@return $newText"
            tagSection?.startsWith("@") ?: false -> "$tagSection $newText"
            tagSection != null -> "@param $tagSection $newText"
            else -> newText
        }

        val inherit =
            when (psiElement) {
                is PsiMethod -> psiElement.findSuperMethods(true).isNotEmpty()
                else -> false
            }
        val initial = if (inherit) "/**\n* {@inheritDoc}\n */" else "/** */"
        val new = insertInto(initial, content, initial.indexOf("*/"))
        if (new.startsWith("/**\n * \n *")) {
            return "/**\n *" + new.substring(10)
        }
        return new
    }

    val doc = trimDocIndent(existingDoc)

    // We'll use the PSI Javadoc support to parse the documentation
    // to help us scan the tokens in the documentation, such that
    // we don't have to search for raw substrings like "@return" which
    // can incorrectly find matches in escaped code snippets etc.
    val factory = JavaPsiFacade.getElementFactory(psiElement.project)
        ?: error("Invalid tool configuration; did not find JavaPsiFacade factory")
    val docComment = factory.createDocCommentFromText(doc)

    if (tagSection == "@return") {
        // Add in return value
        val returnTag = docComment.findTagByName("return")
        if (returnTag == null) {
            // Find last tag
            val lastTag = findLastTag(docComment)
            val offset = if (lastTag != null) {
                findTagEnd(lastTag)
            } else {
                doc.length - 2
            }
            return insertInto(doc, "@return $newText", offset)
        } else {
            // Add text to the existing @return tag
            val offset = if (append)
                findTagEnd(returnTag)
            else returnTag.textRange.startOffset + returnTag.name.length + 1
            return insertInto(doc, newText, offset)
        }
    } else if (tagSection != null) {
        val parameter = if (tagSection.startsWith("@"))
            docComment.findTagByName(tagSection.substring(1))
        else findParamTag(docComment, tagSection)
        if (parameter == null) {
            // Add new parameter or tag
            // TODO: Decide whether to place it alphabetically or place it by parameter order
            // in the signature. Arguably I should follow the convention already present in the
            // doc, if any
            // For now just appending to the last tag before the return tag (if any).
            // This actually works out well in practice where arguments are generally all documented
            // or all not documented; when none of the arguments are documented these end up appending
            // exactly in the right parameter order!
            val returnTag = docComment.findTagByName("return")
            val anchor = returnTag ?: findLastTag(docComment)
            val offset = when {
                returnTag != null -> returnTag.textRange.startOffset
                anchor != null -> findTagEnd(anchor)
                else -> doc.length - 2 // "*/
            }
            val tagName = if (tagSection.startsWith("@")) tagSection else "@param $tagSection"
            return insertInto(doc, "$tagName $newText", offset)
        } else {
            // Add to existing tag/parameter
            val offset = if (append)
                findTagEnd(parameter)
            else parameter.textRange.startOffset + parameter.name.length + 1
            return insertInto(doc, newText, offset)
        }
    } else {
        // Add to the main text section of the comment.
        val firstTag = findFirstTag(docComment)
        val startOffset =
            if (!append) {
                4 // "/** ".length
            } else firstTag?.textRange?.startOffset ?: doc.length - 2
        // Insert a <br> before the appended docs, unless it's the beginning of a doc section
        return insertInto(doc, if (startOffset > 4) "<br>\n$newText" else newText, startOffset)
    }
}

fun findParamTag(docComment: PsiDocComment, paramName: String): PsiDocTag? {
    return docComment.findTagsByName("param").firstOrNull { it.valueElement?.text == paramName }
}

fun findFirstTag(docComment: PsiDocComment): PsiDocTag? {
    return docComment.tags.asSequence().minBy { it.textRange.startOffset }
}

fun findLastTag(docComment: PsiDocComment): PsiDocTag? {
    return docComment.tags.asSequence().maxBy { it.textRange.startOffset }
}

fun findTagEnd(tag: PsiDocTag): Int {
    var curr: PsiElement? = tag.nextSibling
    while (curr != null) {
        if (curr is PsiDocToken && curr.tokenType == JavaDocTokenType.DOC_COMMENT_END) {
            return curr.textRange.startOffset
        } else if (curr is PsiDocTag) {
            return curr.textRange.startOffset
        }

        curr = curr.nextSibling
    }

    return tag.textRange.endOffset
}

fun trimDocIndent(existingDoc: String): String {
    val index = existingDoc.indexOf('\n')
    if (index == -1) {
        return existingDoc
    }

    return existingDoc.substring(0, index + 1) +
        existingDoc.substring(index + 1).trimIndent().split('\n').joinToString(separator = "\n") {
            if (!it.startsWith(" ")) {
                " ${it.trimEnd()}"
            } else {
                it.trimEnd()
            }
        }
}

fun insertInto(existingDoc: String, newText: String, initialOffset: Int): String {
    // TODO: Insert "." between existing documentation and new documentation, if necessary.

    val offset = if (initialOffset > 4 && existingDoc.regionMatches(initialOffset - 4, "\n * ", 0, 4, false)) {
        initialOffset - 4
    } else {
        initialOffset
    }
    val index = existingDoc.indexOf('\n')
    val prefixWithStar = index == -1 || existingDoc[index + 1] == '*' ||
        existingDoc[index + 1] == ' ' && existingDoc[index + 2] == '*'

    val prefix = existingDoc.substring(0, offset)
    val suffix = existingDoc.substring(offset)
    val startSeparator = "\n"
    val endSeparator =
        if (suffix.startsWith("\n") || suffix.startsWith(" \n")) "" else if (suffix == "*/") "\n" else if (prefixWithStar) "\n * " else "\n"

    val middle = if (prefixWithStar) {
        startSeparator + newText.split('\n').joinToString(separator = "\n") { " * $it" } +
            endSeparator
    } else {
        "$startSeparator$newText$endSeparator"
    }

    // Going from single-line to multi-line?
    return if (existingDoc.indexOf('\n') == -1 && existingDoc.startsWith("/** ")) {
        prefix.substring(0, 3) + "\n *" + prefix.substring(3) + middle +
            if (suffix == "*/") " */" else suffix
    } else {
        prefix + middle + suffix
    }
}

/** Converts from package.html content to a package-info.java javadoc string. */
@Language("JAVA")
fun packageHtmlToJavadoc(@Language("HTML") packageHtml: String?): String {
    packageHtml ?: return ""
    if (packageHtml.isBlank()) {
        return ""
    }

    val body = getBodyContents(packageHtml).trim()
    if (body.isBlank()) {
        return ""
    }
    // Combine into comment lines prefixed by asterisk, ,and make sure we don't
    // have end-comment markers in the HTML that will escape out of the javadoc comment
    val comment = body.lines().joinToString(separator = "\n") { " * $it" }.replace("*/", "&#42;/")
    return "/**\n$comment\n */\n"
}

/**
 * Returns the body content from the given HTML document.
 * Attempts to tokenize the HTML properly such that it doesn't
 * get confused by comments or text that looks like tags.
 */
@Suppress("LocalVariableName")
private fun getBodyContents(html: String): String {
    val length = html.length
    val STATE_TEXT = 1
    val STATE_SLASH = 2
    val STATE_ATTRIBUTE_NAME = 3
    val STATE_IN_TAG = 4
    val STATE_BEFORE_ATTRIBUTE = 5
    val STATE_ATTRIBUTE_BEFORE_EQUALS = 6
    val STATE_ATTRIBUTE_AFTER_EQUALS = 7
    val STATE_ATTRIBUTE_VALUE_NONE = 8
    val STATE_ATTRIBUTE_VALUE_SINGLE = 9
    val STATE_ATTRIBUTE_VALUE_DOUBLE = 10
    val STATE_CLOSE_TAG = 11
    val STATE_ENDING_TAG = 12

    var bodyStart = -1
    var htmlStart = -1

    var state = STATE_TEXT
    var offset = 0
    var tagStart = -1
    var tagEndStart = -1
    var prev = -1
    loop@ while (offset < length) {
        if (offset == prev) {
            // Purely here to prevent potential bugs in the state machine from looping
            // infinitely
            offset++
            if (offset == length) {
                break
            }
        }
        prev = offset

        val c = html[offset]
        when (state) {
            STATE_TEXT -> {
                if (c == '<') {
                    state = STATE_SLASH
                    offset++
                    continue@loop
                }

                // Other text is just ignored
                offset++
            }

            STATE_SLASH -> {
                if (c == '!') {
                    if (html.startsWith("!--", offset)) {
                        // Comment
                        val end = html.indexOf("-->", offset + 3)
                        if (end == -1) {
                            offset = length
                        } else {
                            offset = end + 3
                            state = STATE_TEXT
                        }
                        continue@loop
                    } else if (html.startsWith("![CDATA[", offset)) {
                        val end = html.indexOf("]]>", offset + 8)
                        if (end == -1) {
                            offset = length
                        } else {
                            state = STATE_TEXT
                            offset = end + 3
                        }
                        continue@loop
                    } else {
                        val end = html.indexOf('>', offset + 2)
                        if (end == -1) {
                            offset = length
                            state = STATE_TEXT
                        } else {
                            offset = end + 1
                            state = STATE_TEXT
                        }
                        continue@loop
                    }
                } else if (c == '/') {
                    state = STATE_CLOSE_TAG
                    offset++
                    tagEndStart = offset
                    continue@loop
                } else if (c == '?') {
                    // XML Prologue
                    val end = html.indexOf('>', offset + 2)
                    if (end == -1) {
                        offset = length
                        state = STATE_TEXT
                    } else {
                        offset = end + 1
                        state = STATE_TEXT
                    }
                    continue@loop
                }
                state = STATE_IN_TAG
                tagStart = offset
            }

            STATE_CLOSE_TAG -> {
                if (c == '>') {
                    state = STATE_TEXT
                    if (html.startsWith("body", tagEndStart, true)) {
                        val bodyEnd = tagEndStart - 2 // </
                        if (bodyStart != -1) {
                            return html.substring(bodyStart, bodyEnd)
                        }
                    }
                    if (html.startsWith("html", tagEndStart, true)) {
                        val htmlEnd = tagEndStart - 2
                        if (htmlEnd != -1) {
                            return html.substring(htmlStart, htmlEnd)
                        }
                    }
                }
                offset++
            }

            STATE_IN_TAG -> {
                val whitespace = Character.isWhitespace(c)
                if (whitespace || c == '>') {
                    if (html.startsWith("body", tagStart, true)) {
                        bodyStart = html.indexOf('>', offset) + 1
                    }
                    if (html.startsWith("html", tagStart, true)) {
                        htmlStart = html.indexOf('>', offset) + 1
                    }
                }

                when {
                    whitespace -> state = STATE_BEFORE_ATTRIBUTE
                    c == '>' -> {
                        state = STATE_TEXT
                    }
                    c == '/' -> state = STATE_ENDING_TAG
                }
                offset++
            }

            STATE_ENDING_TAG -> {
                if (c == '>') {
                    if (html.startsWith("body", tagEndStart, true)) {
                        val bodyEnd = tagEndStart - 1
                        if (bodyStart != -1) {
                            return html.substring(bodyStart, bodyEnd)
                        }
                    }
                    if (html.startsWith("html", tagEndStart, true)) {
                        val htmlEnd = tagEndStart - 1
                        if (htmlEnd != -1) {
                            return html.substring(htmlStart, htmlEnd)
                        }
                    }
                    offset++
                    state = STATE_TEXT
                }
            }

            STATE_BEFORE_ATTRIBUTE -> {
                if (c == '>') {
                    state = STATE_TEXT
                } else if (c == '/') {
                        // we expect an '>' next to close the tag
                    } else if (!Character.isWhitespace(c)) {
                        state = STATE_ATTRIBUTE_NAME
                    }
                offset++
            }
            STATE_ATTRIBUTE_NAME -> {
                when {
                    c == '>' -> state = STATE_TEXT
                    c == '=' -> state = STATE_ATTRIBUTE_AFTER_EQUALS
                    Character.isWhitespace(c) -> state = STATE_ATTRIBUTE_BEFORE_EQUALS
                    c == ':' -> {
                    }
                }
                offset++
            }
            STATE_ATTRIBUTE_BEFORE_EQUALS -> {
                if (c == '=') {
                    state = STATE_ATTRIBUTE_AFTER_EQUALS
                } else if (c == '>') {
                    state = STATE_TEXT
                } else if (!Character.isWhitespace(c)) {
                    // Attribute value not specified (used for some boolean attributes)
                    state = STATE_ATTRIBUTE_NAME
                }
                offset++
            }

            STATE_ATTRIBUTE_AFTER_EQUALS -> {
                if (c == '\'') {
                    // a='b'
                    state = STATE_ATTRIBUTE_VALUE_SINGLE
                } else if (c == '"') {
                    // a="b"
                    state = STATE_ATTRIBUTE_VALUE_DOUBLE
                } else if (!Character.isWhitespace(c)) {
                    // a=b
                    state = STATE_ATTRIBUTE_VALUE_NONE
                }
                offset++
            }

            STATE_ATTRIBUTE_VALUE_SINGLE -> {
                if (c == '\'') {
                    state = STATE_BEFORE_ATTRIBUTE
                }
                offset++
            }
            STATE_ATTRIBUTE_VALUE_DOUBLE -> {
                if (c == '"') {
                    state = STATE_BEFORE_ATTRIBUTE
                }
                offset++
            }
            STATE_ATTRIBUTE_VALUE_NONE -> {
                if (c == '>') {
                    state = STATE_TEXT
                } else if (Character.isWhitespace(c)) {
                    state = STATE_BEFORE_ATTRIBUTE
                }
                offset++
            }
            else -> assert(false) { state }
        }
    }

    return html
}
