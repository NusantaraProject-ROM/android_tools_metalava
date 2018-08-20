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

package com.android.tools.metalava.model

import com.android.tools.metalava.Severity
import com.android.tools.metalava.doclava1.Errors

/** An error configuration is a set of overrides for severities for various [Errors.Error] */
class ErrorConfiguration {
    private val overrides = mutableMapOf<Errors.Error, Severity>()

    /** Returns the severity of the given issue */
    fun getSeverity(error: Errors.Error): Severity {
        return overrides[error] ?: error.level
    }

    private fun setSeverity(error: Errors.Error, severity: Severity) {
        overrides[error] = severity
    }

    fun error(error: Errors.Error) {
        setSeverity(error, Severity.ERROR)
    }

    fun hide(error: Errors.Error) {
        setSeverity(error, Severity.HIDDEN)
    }

    companion object {
        /** Default error configuration: uses all the severities initialized in the [Errors] class */
        val defaultConfiguration = ErrorConfiguration()

        /**
         * Customization of the severities to apply when doing compatibility checking against the
         * current version of the API. Corresponds to the same flags passed into doclava's error
         * check this way:
         * args: "-error 2 -error 3 -error 4 -error 5 -error 6 " +
         * "-error 7 -error 8 -error 9 -error 10 -error 11 -error 12 -error 13 -error 14 -error 15 " +
         * "-error 16 -error 17 -error 18 -error 19 -error 20 -error 21 -error 23 -error 24 " +
         * "-error 25 -error 26 -error 27",
         */
        val currentCompatibilityCheckConfiguration = ErrorConfiguration().apply {
            error(Errors.ADDED_PACKAGE)
            error(Errors.ADDED_CLASS)
            error(Errors.ADDED_METHOD)
            error(Errors.ADDED_FIELD)
            error(Errors.ADDED_INTERFACE)
            error(Errors.REMOVED_PACKAGE)
            error(Errors.REMOVED_CLASS)
            error(Errors.REMOVED_METHOD)
            error(Errors.REMOVED_FIELD)
            error(Errors.REMOVED_INTERFACE)
            error(Errors.CHANGED_STATIC)
            error(Errors.ADDED_FINAL)
            error(Errors.CHANGED_TRANSIENT)
            error(Errors.CHANGED_VOLATILE)
            error(Errors.CHANGED_TYPE)
            error(Errors.CHANGED_VALUE)
            error(Errors.CHANGED_SUPERCLASS)
            error(Errors.CHANGED_SCOPE)
            error(Errors.CHANGED_ABSTRACT)
            error(Errors.CHANGED_THROWS)
            error(Errors.CHANGED_CLASS)
            error(Errors.CHANGED_DEPRECATED)
            error(Errors.CHANGED_SYNCHRONIZED)
            error(Errors.ADDED_FINAL_UNINSTANTIABLE)
            error(Errors.REMOVED_FINAL)
        }

        /**
         * Customization of the severities to apply when doing compatibility checking against the
         * previously released stable version of the API. Corresponds to the same flags passed into
         * doclava's error check this way:
         * args: "-hide 2 -hide 3 -hide 4 -hide 5 -hide 6 -hide 24 -hide 25 -hide 26 -hide 27 " +
         * "-error 7 -error 8 -error 9 -error 10 -error 11 -error 12 -error 13 -error 14 -error 15 " +
         * "-error 16 -error 17 -error 18 -error 31",
         */
        val releasedCompatibilityCheckConfiguration = ErrorConfiguration().apply {
            hide(Errors.ADDED_PACKAGE)
            hide(Errors.ADDED_CLASS)
            hide(Errors.ADDED_METHOD)
            hide(Errors.ADDED_FIELD)
            hide(Errors.ADDED_INTERFACE)
            hide(Errors.CHANGED_DEPRECATED)
            hide(Errors.CHANGED_SYNCHRONIZED)
            hide(Errors.ADDED_FINAL_UNINSTANTIABLE)
            hide(Errors.REMOVED_FINAL)
            error(Errors.REMOVED_PACKAGE)
            error(Errors.REMOVED_CLASS)
            error(Errors.REMOVED_METHOD)
            error(Errors.REMOVED_FIELD)
            error(Errors.REMOVED_INTERFACE)
            error(Errors.CHANGED_STATIC)
            error(Errors.ADDED_FINAL)
            error(Errors.CHANGED_TRANSIENT)
            error(Errors.CHANGED_VOLATILE)
            error(Errors.CHANGED_TYPE)
            error(Errors.CHANGED_VALUE)
            error(Errors.CHANGED_SUPERCLASS)
            error(Errors.ADDED_ABSTRACT_METHOD)
        }
    }
}

/** Current configuration to apply when reporting errors */
var configuration = ErrorConfiguration.defaultConfiguration