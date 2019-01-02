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

package com.android.tools.metalava

/** Current signature format. */
const val CURRENT_SIGNATURE_FORMAT = "2.0"

/** Marker comment at the beginning of the signature file */
const val SIGNATURE_FORMAT_PREFIX = "// Signature format: "

/** File formats that metalava can emit APIs to */
enum class FileFormat(val version: String) {
    UNKNOWN("?"),
    JDIFF("JDiff"),
    V1("1.0"),
    V2("2.0"),
    V3("3.0");

    /** Configures the option object such that the output format will be the given format */
    fun configureOptions(options: Options) {
        if (this == JDIFF) {
            return
        }
        options.outputFormat = this
        options.compatOutput = this == V1
        options.outputKotlinStyleNulls = this >= V3
        options.outputDefaultValues = this >= V2
        options.omitCommonPackages = this >= V2
        options.includeSignatureFormatVersion = this >= V2
    }

    fun useKotlinStyleNulls(): Boolean {
        return this >= V3
    }
    companion object {
        fun parse(version: String): FileFormat {
            for (format in values()) {
                if (format.version == version) {
                    return format
                }
            }
            return UNKNOWN
        }
    }
}