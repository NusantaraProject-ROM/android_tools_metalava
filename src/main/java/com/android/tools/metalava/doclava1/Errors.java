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
package com.android.tools.metalava.doclava1;

import com.android.tools.metalava.Severity;
import com.google.common.base.Splitter;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.android.sdklib.SdkVersionInfo.underlinesToCamelCase;
import static com.android.tools.metalava.Severity.ERROR;
import static com.android.tools.metalava.Severity.HIDDEN;
import static com.android.tools.metalava.Severity.INHERIT;
import static com.android.tools.metalava.Severity.LINT;
import static com.android.tools.metalava.Severity.WARNING;

// Copied from doclava1 (and a bunch of stuff left alone; preserving to have same error id's)
public class Errors {
    // Consider renaming to Issue; "Error" is special in Kotlin, and what does it mean for
    // an "error" to have severity "warning" ? The severity shouldn't be implied by the name.
    public static class Error {
        public final int code;
        @Nullable
        public String fieldName;

        private Severity level;
        private final Severity defaultLevel;
        public boolean setByUser;

        /**
         * The name of this error if known
         */
        @Nullable
        public String name;

        /**
         * When {@code level} is set to {@link Severity#INHERIT}, this is the parent from
         * which the error will inherit its level.
         */
        private final Error parent;

        Error(int code, Severity level) {
            this.code = code;
            this.level = level;
            this.defaultLevel = level;
            this.parent = null;
            errors.add(this);
        }

        Error(int code, Error parent) {
            this.code = code;
            this.level = Severity.INHERIT;
            this.defaultLevel = Severity.INHERIT;
            this.parent = parent;
            errors.add(this);
        }

        /**
         * Returns the implied level for this error.
         * <p>
         * If the level is {@link Severity#INHERIT}, the level will be returned for the
         * parent.
         *
         * @throws IllegalStateException if the level is {@link Severity#INHERIT} and the
         *                               parent is {@code null}
         */
        public Severity getLevel() {
            if (level == INHERIT) {
                if (parent == null) {
                    throw new IllegalStateException("Error with level INHERIT must have non-null parent");
                }
                return parent.getLevel();
            }
            return level;
        }

        /**
         * Sets the level.
         * <p>
         * Valid arguments are:
         * <ul>
         * <li>{@link Severity#HIDDEN}
         * <li>{@link Severity#WARNING}
         * <li>{@link Severity#ERROR}
         * </ul>
         *
         * @param level the level to set
         */
        void setLevel(Severity level) {
            if (level == INHERIT) {
                throw new IllegalArgumentException("Error level may not be set to INHERIT");
            }
            this.level = level;
            this.setByUser = true;
        }

        public String toString() {
            return "Error #" + this.code + " (" + this.name + ")";
        }
    }

    private static final List<Error> errors = new ArrayList<>(100);
    private static final Map<String, Error> nameToError = new HashMap<>(100);
    private static final Map<Integer, Error> idToError = new HashMap<>(100);

    // Errors for API verification
    public static final Error PARSE_ERROR = new Error(1, ERROR);
    public static final Error ADDED_PACKAGE = new Error(2, WARNING);
    public static final Error ADDED_CLASS = new Error(3, WARNING);
    public static final Error ADDED_METHOD = new Error(4, WARNING);
    public static final Error ADDED_FIELD = new Error(5, WARNING);
    public static final Error ADDED_INTERFACE = new Error(6, WARNING);
    public static final Error REMOVED_PACKAGE = new Error(7, WARNING);
    public static final Error REMOVED_CLASS = new Error(8, WARNING);
    public static final Error REMOVED_METHOD = new Error(9, WARNING);
    public static final Error REMOVED_FIELD = new Error(10, WARNING);
    public static final Error REMOVED_INTERFACE = new Error(11, WARNING);
    public static final Error CHANGED_STATIC = new Error(12, WARNING);
    public static final Error ADDED_FINAL = new Error(13, WARNING);
    public static final Error CHANGED_TRANSIENT = new Error(14, WARNING);
    public static final Error CHANGED_VOLATILE = new Error(15, WARNING);
    public static final Error CHANGED_TYPE = new Error(16, WARNING);
    public static final Error CHANGED_VALUE = new Error(17, WARNING);
    public static final Error CHANGED_SUPERCLASS = new Error(18, WARNING);
    public static final Error CHANGED_SCOPE = new Error(19, WARNING);
    public static final Error CHANGED_ABSTRACT = new Error(20, WARNING);
    public static final Error CHANGED_THROWS = new Error(21, WARNING);
    public static final Error CHANGED_NATIVE = new Error(22, HIDDEN);
    public static final Error CHANGED_CLASS = new Error(23, WARNING);
    public static final Error CHANGED_DEPRECATED = new Error(24, WARNING);
    public static final Error CHANGED_SYNCHRONIZED = new Error(25, WARNING);
    public static final Error ADDED_FINAL_UNINSTANTIABLE = new Error(26, WARNING);
    public static final Error REMOVED_FINAL = new Error(27, WARNING);
    public static final Error REMOVED_DEPRECATED_CLASS = new Error(28, REMOVED_CLASS);
    public static final Error REMOVED_DEPRECATED_METHOD = new Error(29, REMOVED_METHOD);
    public static final Error REMOVED_DEPRECATED_FIELD = new Error(30, REMOVED_FIELD);
    public static final Error ADDED_ABSTRACT_METHOD = new Error(31, ADDED_METHOD);

    // Errors in javadoc generation
    public static final Error UNRESOLVED_LINK = new Error(101, LINT);
    public static final Error BAD_INCLUDE_TAG = new Error(102, LINT);
    public static final Error UNKNOWN_TAG = new Error(103, LINT);
    public static final Error UNKNOWN_PARAM_TAG_NAME = new Error(104, LINT);
    public static final Error UNDOCUMENTED_PARAMETER = new Error(105, HIDDEN); // LINT
    public static final Error BAD_ATTR_TAG = new Error(106, LINT);
    public static final Error BAD_INHERITDOC = new Error(107, HIDDEN); // LINT
    public static final Error HIDDEN_LINK = new Error(108, LINT);
    public static final Error HIDDEN_CONSTRUCTOR = new Error(109, WARNING);
    public static final Error UNAVAILABLE_SYMBOL = new Error(110, WARNING);
    public static final Error HIDDEN_SUPERCLASS = new Error(111, WARNING);
    public static final Error DEPRECATED = new Error(112, HIDDEN);
    public static final Error DEPRECATION_MISMATCH = new Error(113, WARNING);
    public static final Error MISSING_COMMENT = new Error(114, LINT);
    public static final Error IO_ERROR = new Error(115, ERROR);
    public static final Error NO_SINCE_DATA = new Error(116, HIDDEN);
    public static final Error NO_FEDERATION_DATA = new Error(117, WARNING);
    public static final Error BROKEN_SINCE_FILE = new Error(118, ERROR);
    public static final Error INVALID_CONTENT_TYPE = new Error(119, ERROR);
    public static final Error INVALID_SAMPLE_INDEX = new Error(120, ERROR);
    public static final Error HIDDEN_TYPE_PARAMETER = new Error(121, WARNING);
    public static final Error PRIVATE_SUPERCLASS = new Error(122, WARNING);
    public static final Error NULLABLE = new Error(123, HIDDEN); // LINT
    public static final Error INT_DEF = new Error(124, HIDDEN); // LINT
    public static final Error REQUIRES_PERMISSION = new Error(125, LINT);
    public static final Error BROADCAST_BEHAVIOR = new Error(126, LINT);
    public static final Error SDK_CONSTANT = new Error(127, LINT);
    public static final Error TODO = new Error(128, LINT);
    public static final Error NO_ARTIFACT_DATA = new Error(129, HIDDEN);
    public static final Error BROKEN_ARTIFACT_FILE = new Error(130, ERROR);

    // Metalava new warnings (not from doclava)

    public static final Error TYPO = new Error(131, WARNING);
    public static final Error MISSING_PERMISSION = new Error(132, LINT);
    public static final Error MULTIPLE_THREAD_ANNOTATIONS = new Error(133, LINT);
    public static final Error UNRESOLVED_CLASS = new Error(134, LINT);
    public static final Error INVALID_NULL_CONVERSION = new Error(135, ERROR);
    public static final Error PARAMETER_NAME_CHANGE = new Error(136, ERROR);
    public static final Error OPERATOR_REMOVAL = new Error(137, ERROR);
    public static final Error INFIX_REMOVAL = new Error(138, ERROR);
    public static final Error VARARG_REMOVAL = new Error(139, ERROR);
    public static final Error ADD_SEALED = new Error(140, ERROR);
    public static final Error KOTLIN_KEYWORD = new Error(141, WARNING);
    public static final Error SAM_SHOULD_BE_LAST = new Error(142, WARNING);
    public static final Error MISSING_JVMSTATIC = new Error(143, WARNING);
    public static final Error DEFAULT_VALUE_CHANGE = new Error(144, ERROR);
    public static final Error DOCUMENT_EXCEPTIONS = new Error(145, ERROR);
    public static final Error ANNOTATION_EXTRACTION = new Error(146, ERROR);
    public static final Error SUPERFLUOUS_PREFIX = new Error(147, WARNING);
    public static final Error HIDDEN_TYPEDEF_CONSTANT = new Error(148, ERROR);
    public static final Error EXPECTED_PLATFORM_TYPE = new Error(149, HIDDEN);
    public static final Error INTERNAL_ERROR = new Error(150, ERROR);
    public static final Error RETURNING_UNEXPECTED_CONSTANT = new Error(151, WARNING);
    public static final Error DEPRECATED_OPTION = new Error(152, WARNING);
    public static final Error BOTH_PACKAGE_INFO_AND_HTML = new Error(153, WARNING);
    // The plan is for this to be set as an error once (1) existing code is marked as @deprecated
    // and (2) the principle is adopted by the API council
    public static final Error REFERENCES_DEPRECATED = new Error(154, HIDDEN);
    // Hidden until (1) API council agrees this should be an error, and (2) existing
    // violations are annotated as @hide
    public static final Error UNHIDDEN_SYSTEM_API = new Error(155, HIDDEN);
    public static final Error SHOWING_MEMBER_IN_HIDDEN_CLASS = new Error(156, ERROR);

    static {
        // Attempt to initialize error names based on the field names
        try {
            for (Field field : Errors.class.getDeclaredFields()) {
                Object o = field.get(null);
                if (o instanceof Error) {
                    Error error = (Error) o;
                    String fieldName = field.getName();
                    error.fieldName = fieldName;
                    error.name = underlinesToCamelCase(fieldName.toLowerCase(Locale.US));
                    nameToError.put(error.name, error);
                    idToError.put(error.code, error);
                }
            }
        } catch (Throwable unexpected) {
            unexpected.printStackTrace();
        }
    }

    @Nullable
    public static Error findErrorById(int id) {
        return idToError.get(id);
    }

    public static boolean setErrorLevel(String id, Severity level, boolean setByUser) {
        if (id.contains(",")) { // Handle being passed in multiple comma separated id's
            boolean ok = true;
            for (String individualId : Splitter.on(',').trimResults().split(id)) {
                ok = setErrorLevel(individualId, level, setByUser) && ok;
            }
            return ok;
        }
        int code = -1;
        if (Character.isDigit(id.charAt(0))) {
            code = Integer.parseInt(id);
        }

        Error error = nameToError.get(id);
        if (error == null) {
            try {
                int n = Integer.parseInt(id);
                error = idToError.get(n);
            } catch (NumberFormatException ignore) {
            }
        }

        if (error == null) {
            for (Error e : errors) {
                if (e.code == code || id.equalsIgnoreCase(e.name)) {
                    error = e;
                    break;
                }
            }
        }

        if (error != null) {
            error.setLevel(level);
            error.setByUser = setByUser;
            return true;
        }
        return false;
    }

    // Primary needed by unit tests; ensure that a previous test doesn't influence
    // a later one
    public static void resetLevels() {
        for (Error error : errors) {
            error.level = error.defaultLevel;
        }
    }
}
