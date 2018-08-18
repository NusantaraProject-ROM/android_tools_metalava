/*
 * Copyright (C) 2018 The Android Open Source Project
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

import org.intellij.lang.annotations.Language
import org.junit.Assert.assertEquals
import org.junit.Test

class JavadocTest {
    @Test
    fun `Test package to package info`() {
        @Language("HTML")
        val html = """
            <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 3.2 Final//EN">
            <!-- not a body tag: <body> -->
            <html>
            <body bgcolor="white">
            My package docs<br>
            <!-- comment -->
            Sample code: /** code here */
            Another line.<br>
            </BODY>
            </html>
            """

        @Language("JAVA")
        val java = """
            /**
             * My package docs<br>
             * <!-- comment -->
             * Sample code: /** code here &#42;/
             * Another line.<br>
             */
            """

        assertEquals(java.trimIndent() + "\n", packageHtmlToJavadoc(html.trimIndent()))
    }
}