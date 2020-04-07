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

// Copied from doclava1 (and a bunch of stuff left alone; preserving to have same error id's)
public class Issues {
    public static class Issue {
        public final int code;
        @Nullable
        String fieldName;

        private Severity level;
        private final Severity defaultLevel;
        boolean setByUser;

        /**
         * The name of this issue if known
         */
        @Nullable
        public String name;

        /**
         * When {@code level} is set to {@link Severity#INHERIT}, this is the parent from
         * which the issue will inherit its level.
         */
        private final Issue parent;

        /** Related rule, if any */
        public final String rule;

        /** Related explanation, if any */
        public final String explanation;

        /** Applicable category */
        public final Category category;

        private Issue(int code, Severity level) {
            this(code, level, Category.UNKNOWN);
        }

        private Issue(int code, Severity level, Category category) {
            this(code, level, null, category, null, null);
        }

        private Issue(int code, Severity level, Category category, String rule) {
            this(code, level, null, category, rule, null);
        }

        private Issue(int code, Issue parent, Category category) {
            this(code, Severity.INHERIT, parent, category, null, null);
        }

        private Issue(int code, Severity level, Issue parent, Category category,
                      String rule, String explanation) {
            this.code = code;
            this.level = level;
            this.defaultLevel = level;
            this.parent = parent;
            this.category = category;
            this.rule = rule;
            this.explanation = explanation;
            ISSUES.add(this);
        }

        /**
         * Returns the implied level for this issue.
         * <p>
         * If the level is {@link Severity#INHERIT}, the level will be returned for the
         * parent.
         *
         * @throws IllegalStateException if the level is {@link Severity#INHERIT} and the
         *                               parent is {@code null}
         */
        public Severity getLevel() {
            if (level == Severity.INHERIT) {
                if (parent == null) {
                    throw new IllegalStateException("Issue with level INHERIT must have non-null parent");
                }
                return parent.getLevel();
            }
            return level;
        }

        public boolean isInherit() {
            return level == Severity.INHERIT;
        }

        public Issue getParent() {
            return parent;
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
            if (level == Severity.INHERIT) {
                throw new IllegalArgumentException("Issue level may not be set to INHERIT");
            }
            this.level = level;
            this.setByUser = true;
        }

        public String toString() {
            return "Issue #" + this.code + " (" + this.name + ")";
        }
    }

    private static final List<Issue> ISSUES = new ArrayList<>(100);
    private static final Map<String, Issue> nameToIssue = new HashMap<>(100);
    private static final Map<Integer, Issue> idToIssue = new HashMap<>(100);

    public enum Category {
        COMPATIBILITY("Compatibility", null),
        DOCUMENTATION("Documentation", null),
        API_LINT("API Lint", "go/android-api-guidelines"),
        UNKNOWN("Default", null);

        public final String description;
        public final String ruleLink;

        Category(String description, String ruleLink) {
            this.description = description;
            this.ruleLink = ruleLink;
        }
    }

    // Issues for API verification
    public static final Issue PARSE_ERROR = new Issue(1, Severity.ERROR);
    public static final Issue ADDED_PACKAGE = new Issue(2, Severity.WARNING, Category.COMPATIBILITY);
    public static final Issue ADDED_CLASS = new Issue(3, Severity.WARNING, Category.COMPATIBILITY);
    public static final Issue ADDED_METHOD = new Issue(4, Severity.WARNING, Category.COMPATIBILITY);
    public static final Issue ADDED_FIELD = new Issue(5, Severity.WARNING, Category.COMPATIBILITY);
    public static final Issue ADDED_INTERFACE = new Issue(6, Severity.WARNING, Category.COMPATIBILITY);
    public static final Issue REMOVED_PACKAGE = new Issue(7, Severity.WARNING, Category.COMPATIBILITY);
    public static final Issue REMOVED_CLASS = new Issue(8, Severity.WARNING, Category.COMPATIBILITY);
    public static final Issue REMOVED_METHOD = new Issue(9, Severity.WARNING, Category.COMPATIBILITY);
    public static final Issue REMOVED_FIELD = new Issue(10, Severity.WARNING, Category.COMPATIBILITY);
    public static final Issue REMOVED_INTERFACE = new Issue(11, Severity.WARNING, Category.COMPATIBILITY);
    public static final Issue CHANGED_STATIC = new Issue(12, Severity.WARNING, Category.COMPATIBILITY);
    public static final Issue ADDED_FINAL = new Issue(13, Severity.WARNING, Category.COMPATIBILITY);
    public static final Issue CHANGED_TRANSIENT = new Issue(14, Severity.WARNING, Category.COMPATIBILITY);
    public static final Issue CHANGED_VOLATILE = new Issue(15, Severity.WARNING, Category.COMPATIBILITY);
    public static final Issue CHANGED_TYPE = new Issue(16, Severity.WARNING, Category.COMPATIBILITY);
    public static final Issue CHANGED_VALUE = new Issue(17, Severity.WARNING, Category.COMPATIBILITY);
    public static final Issue CHANGED_SUPERCLASS = new Issue(18, Severity.WARNING, Category.COMPATIBILITY);
    public static final Issue CHANGED_SCOPE = new Issue(19, Severity.WARNING, Category.COMPATIBILITY);
    public static final Issue CHANGED_ABSTRACT = new Issue(20, Severity.WARNING, Category.COMPATIBILITY);
    public static final Issue CHANGED_THROWS = new Issue(21, Severity.WARNING, Category.COMPATIBILITY);
    public static final Issue CHANGED_NATIVE = new Issue(22, Severity.HIDDEN, Category.COMPATIBILITY);
    public static final Issue CHANGED_CLASS = new Issue(23, Severity.WARNING, Category.COMPATIBILITY);
    public static final Issue CHANGED_DEPRECATED = new Issue(24, Severity.WARNING, Category.COMPATIBILITY);
    public static final Issue CHANGED_SYNCHRONIZED = new Issue(25, Severity.WARNING, Category.COMPATIBILITY);
    public static final Issue ADDED_FINAL_UNINSTANTIABLE = new Issue(26, Severity.WARNING, Category.COMPATIBILITY);
    public static final Issue REMOVED_FINAL = new Issue(27, Severity.WARNING, Category.COMPATIBILITY);
    public static final Issue REMOVED_DEPRECATED_CLASS = new Issue(28, REMOVED_CLASS, Category.COMPATIBILITY);
    public static final Issue REMOVED_DEPRECATED_METHOD = new Issue(29, REMOVED_METHOD, Category.COMPATIBILITY);
    public static final Issue REMOVED_DEPRECATED_FIELD = new Issue(30, REMOVED_FIELD, Category.COMPATIBILITY);
    public static final Issue ADDED_ABSTRACT_METHOD = new Issue(31, ADDED_METHOD, Category.COMPATIBILITY);
    public static final Issue ADDED_REIFIED = new Issue(32, Severity.WARNING, Category.COMPATIBILITY);

    // Issues in javadoc generation
    public static final Issue UNRESOLVED_LINK = new Issue(101, Severity.LINT, Category.DOCUMENTATION);
    public static final Issue BAD_INCLUDE_TAG = new Issue(102, Severity.LINT, Category.DOCUMENTATION);
    public static final Issue UNKNOWN_TAG = new Issue(103, Severity.LINT, Category.DOCUMENTATION);
    public static final Issue UNKNOWN_PARAM_TAG_NAME = new Issue(104, Severity.LINT, Category.DOCUMENTATION);
    public static final Issue UNDOCUMENTED_PARAMETER = new Issue(105, Severity.HIDDEN, Category.DOCUMENTATION);
    public static final Issue BAD_ATTR_TAG = new Issue(106, Severity.LINT, Category.DOCUMENTATION);
    public static final Issue BAD_INHERITDOC = new Issue(107, Severity.HIDDEN, Category.DOCUMENTATION);
    public static final Issue HIDDEN_LINK = new Issue(108, Severity.LINT, Category.DOCUMENTATION);
    public static final Issue HIDDEN_CONSTRUCTOR = new Issue(109, Severity.WARNING, Category.DOCUMENTATION);
    public static final Issue UNAVAILABLE_SYMBOL = new Issue(110, Severity.WARNING, Category.DOCUMENTATION);
    public static final Issue HIDDEN_SUPERCLASS = new Issue(111, Severity.WARNING, Category.DOCUMENTATION);
    public static final Issue DEPRECATED = new Issue(112, Severity.HIDDEN, Category.DOCUMENTATION);
    public static final Issue DEPRECATION_MISMATCH = new Issue(113, Severity.ERROR, Category.DOCUMENTATION);
    public static final Issue MISSING_COMMENT = new Issue(114, Severity.LINT, Category.DOCUMENTATION);
    public static final Issue IO_ERROR = new Issue(115, Severity.ERROR);
    public static final Issue NO_SINCE_DATA = new Issue(116, Severity.HIDDEN, Category.DOCUMENTATION);
    public static final Issue NO_FEDERATION_DATA = new Issue(117, Severity.WARNING, Category.DOCUMENTATION);
    public static final Issue BROKEN_SINCE_FILE = new Issue(118, Severity.ERROR, Category.DOCUMENTATION);
    public static final Issue INVALID_CONTENT_TYPE = new Issue(119, Severity.ERROR, Category.DOCUMENTATION);
    public static final Issue INVALID_SAMPLE_INDEX = new Issue(120, Severity.ERROR, Category.DOCUMENTATION);
    public static final Issue HIDDEN_TYPE_PARAMETER = new Issue(121, Severity.WARNING, Category.DOCUMENTATION);
    public static final Issue PRIVATE_SUPERCLASS = new Issue(122, Severity.WARNING, Category.DOCUMENTATION);
    public static final Issue NULLABLE = new Issue(123, Severity.HIDDEN, Category.DOCUMENTATION);
    public static final Issue INT_DEF = new Issue(124, Severity.HIDDEN, Category.DOCUMENTATION);
    public static final Issue REQUIRES_PERMISSION = new Issue(125, Severity.LINT, Category.DOCUMENTATION);
    public static final Issue BROADCAST_BEHAVIOR = new Issue(126, Severity.LINT, Category.DOCUMENTATION);
    public static final Issue SDK_CONSTANT = new Issue(127, Severity.LINT, Category.DOCUMENTATION);
    public static final Issue TODO = new Issue(128, Severity.LINT, Category.DOCUMENTATION);
    public static final Issue NO_ARTIFACT_DATA = new Issue(129, Severity.HIDDEN, Category.DOCUMENTATION);
    public static final Issue BROKEN_ARTIFACT_FILE = new Issue(130, Severity.ERROR, Category.DOCUMENTATION);

    // Metalava new warnings (not from doclava)

    public static final Issue TYPO = new Issue(131, Severity.WARNING, Category.DOCUMENTATION);
    public static final Issue MISSING_PERMISSION = new Issue(132, Severity.LINT, Category.DOCUMENTATION);
    public static final Issue MULTIPLE_THREAD_ANNOTATIONS = new Issue(133, Severity.LINT, Category.DOCUMENTATION);
    public static final Issue UNRESOLVED_CLASS = new Issue(134, Severity.LINT, Category.DOCUMENTATION);
    public static final Issue INVALID_NULL_CONVERSION = new Issue(135, Severity.ERROR, Category.COMPATIBILITY);
    public static final Issue PARAMETER_NAME_CHANGE = new Issue(136, Severity.ERROR, Category.COMPATIBILITY);
    public static final Issue OPERATOR_REMOVAL = new Issue(137, Severity.ERROR, Category.COMPATIBILITY);
    public static final Issue INFIX_REMOVAL = new Issue(138, Severity.ERROR, Category.COMPATIBILITY);
    public static final Issue VARARG_REMOVAL = new Issue(139, Severity.ERROR, Category.COMPATIBILITY);
    public static final Issue ADD_SEALED = new Issue(140, Severity.ERROR, Category.COMPATIBILITY);
    public static final Issue ANNOTATION_EXTRACTION = new Issue(146, Severity.ERROR);
    public static final Issue SUPERFLUOUS_PREFIX = new Issue(147, Severity.WARNING);
    public static final Issue HIDDEN_TYPEDEF_CONSTANT = new Issue(148, Severity.ERROR);
    public static final Issue EXPECTED_PLATFORM_TYPE = new Issue(149, Severity.HIDDEN);
    public static final Issue INTERNAL_ERROR = new Issue(150, Severity.ERROR);
    public static final Issue RETURNING_UNEXPECTED_CONSTANT = new Issue(151, Severity.WARNING);
    public static final Issue DEPRECATED_OPTION = new Issue(152, Severity.WARNING);
    public static final Issue BOTH_PACKAGE_INFO_AND_HTML = new Issue(153, Severity.WARNING, Category.DOCUMENTATION);
    // The plan is for this to be set as an error once (1) existing code is marked as @deprecated
    // and (2) the principle is adopted by the API council
    public static final Issue REFERENCES_DEPRECATED = new Issue(154, Severity.HIDDEN);
    public static final Issue UNHIDDEN_SYSTEM_API = new Issue(155, Severity.ERROR);
    public static final Issue SHOWING_MEMBER_IN_HIDDEN_CLASS = new Issue(156, Severity.ERROR);
    public static final Issue INVALID_NULLABILITY_ANNOTATION = new Issue(157, Severity.ERROR);
    public static final Issue REFERENCES_HIDDEN = new Issue(158, Severity.ERROR);
    public static final Issue IGNORING_SYMLINK = new Issue(159, Severity.INFO);
    public static final Issue INVALID_NULLABILITY_ANNOTATION_WARNING = new Issue(160, Severity.WARNING);
    // The plan is for this to be set as an error once (1) existing code is marked as @deprecated
    // and (2) the principle is adopted by the API council
    public static final Issue EXTENDS_DEPRECATED = new Issue(161, Severity.HIDDEN);
    public static final Issue FORBIDDEN_TAG = new Issue(162, Severity.ERROR);
    public static final Issue MISSING_COLUMN = new Issue(163, Severity.WARNING, Category.DOCUMENTATION);
    public static final Issue INVALID_SYNTAX = new Issue(164, Severity.ERROR);

    // API lint
    public static final Issue START_WITH_LOWER = new Issue(300, Severity.ERROR, Category.API_LINT, "S1");
    public static final Issue START_WITH_UPPER = new Issue(301, Severity.ERROR, Category.API_LINT, "S1");
    public static final Issue ALL_UPPER = new Issue(302, Severity.ERROR, Category.API_LINT, "C2");
    public static final Issue ACRONYM_NAME = new Issue(303, Severity.WARNING, Category.API_LINT, "S1");
    public static final Issue ENUM = new Issue(304, Severity.ERROR, Category.API_LINT, "F5");
    public static final Issue ENDS_WITH_IMPL = new Issue(305, Severity.ERROR, Category.API_LINT);
    public static final Issue MIN_MAX_CONSTANT = new Issue(306, Severity.WARNING, Category.API_LINT, "C8");
    public static final Issue COMPILE_TIME_CONSTANT = new Issue(307, Severity.ERROR, Category.API_LINT);
    public static final Issue SINGULAR_CALLBACK = new Issue(308, Severity.ERROR, Category.API_LINT, "L1");
    public static final Issue CALLBACK_NAME = new Issue(309, Severity.WARNING, Category.API_LINT, "L1");
    public static final Issue CALLBACK_INTERFACE = new Issue(310, Severity.ERROR, Category.API_LINT, "CL3");
    public static final Issue CALLBACK_METHOD_NAME = new Issue(311, Severity.ERROR, Category.API_LINT, "L1");
    public static final Issue LISTENER_INTERFACE = new Issue(312, Severity.ERROR, Category.API_LINT, "L1");
    public static final Issue SINGLE_METHOD_INTERFACE = new Issue(313, Severity.ERROR, Category.API_LINT, "L1");
    public static final Issue INTENT_NAME = new Issue(314, Severity.ERROR, Category.API_LINT, "C3");
    public static final Issue ACTION_VALUE = new Issue(315, Severity.ERROR, Category.API_LINT, "C4");
    public static final Issue EQUALS_AND_HASH_CODE = new Issue(316, Severity.ERROR, Category.API_LINT, "M8");
    public static final Issue PARCEL_CREATOR = new Issue(317, Severity.ERROR, Category.API_LINT, "FW3");
    public static final Issue PARCEL_NOT_FINAL = new Issue(318, Severity.ERROR, Category.API_LINT, "FW8");
    public static final Issue PARCEL_CONSTRUCTOR = new Issue(319, Severity.ERROR, Category.API_LINT, "FW3");
    public static final Issue PROTECTED_MEMBER = new Issue(320, Severity.ERROR, Category.API_LINT, "M7");
    public static final Issue PAIRED_REGISTRATION = new Issue(321, Severity.ERROR, Category.API_LINT, "L2");
    public static final Issue REGISTRATION_NAME = new Issue(322, Severity.ERROR, Category.API_LINT, "L3");
    public static final Issue VISIBLY_SYNCHRONIZED = new Issue(323, Severity.ERROR, Category.API_LINT, "M5");
    public static final Issue INTENT_BUILDER_NAME = new Issue(324, Severity.WARNING, Category.API_LINT, "FW1");
    public static final Issue CONTEXT_NAME_SUFFIX = new Issue(325, Severity.ERROR, Category.API_LINT, "C4");
    public static final Issue INTERFACE_CONSTANT = new Issue(326, Severity.ERROR, Category.API_LINT, "C4");
    public static final Issue ON_NAME_EXPECTED = new Issue(327, Severity.WARNING, Category.API_LINT);
    public static final Issue TOP_LEVEL_BUILDER = new Issue(328, Severity.WARNING, Category.API_LINT);
    public static final Issue MISSING_BUILD_METHOD = new Issue(329, Severity.WARNING, Category.API_LINT);
    public static final Issue BUILDER_SET_STYLE = new Issue(330, Severity.WARNING, Category.API_LINT);
    public static final Issue SETTER_RETURNS_THIS = new Issue(331, Severity.WARNING, Category.API_LINT, "M4");
    public static final Issue RAW_AIDL = new Issue(332, Severity.ERROR, Category.API_LINT);
    public static final Issue INTERNAL_CLASSES = new Issue(333, Severity.ERROR, Category.API_LINT);
    public static final Issue PACKAGE_LAYERING = new Issue(334, Severity.WARNING, Category.API_LINT, "FW6");
    public static final Issue GETTER_SETTER_NAMES = new Issue(335, Severity.ERROR, Category.API_LINT, "M6");
    public static final Issue CONCRETE_COLLECTION = new Issue(336, Severity.ERROR, Category.API_LINT, "CL2");
    public static final Issue OVERLAPPING_CONSTANTS = new Issue(337, Severity.WARNING, Category.API_LINT, "C1");
    public static final Issue GENERIC_EXCEPTION = new Issue(338, Severity.ERROR, Category.API_LINT, "S1");
    public static final Issue ILLEGAL_STATE_EXCEPTION = new Issue(339, Severity.WARNING, Category.API_LINT, "S1");
    public static final Issue RETHROW_REMOTE_EXCEPTION = new Issue(340, Severity.ERROR, Category.API_LINT, "FW9");
    public static final Issue MENTIONS_GOOGLE = new Issue(341, Severity.ERROR, Category.API_LINT);
    public static final Issue HEAVY_BIT_SET = new Issue(342, Severity.ERROR, Category.API_LINT);
    public static final Issue MANAGER_CONSTRUCTOR = new Issue(343, Severity.ERROR, Category.API_LINT);
    public static final Issue MANAGER_LOOKUP = new Issue(344, Severity.ERROR, Category.API_LINT);
    public static final Issue AUTO_BOXING = new Issue(345, Severity.ERROR, Category.API_LINT, "M11");
    public static final Issue STATIC_UTILS = new Issue(346, Severity.ERROR, Category.API_LINT);
    public static final Issue CONTEXT_FIRST = new Issue(347, Severity.ERROR, Category.API_LINT, "M3");
    public static final Issue LISTENER_LAST = new Issue(348, Severity.WARNING, Category.API_LINT, "M3");
    public static final Issue EXECUTOR_REGISTRATION = new Issue(349, Severity.WARNING, Category.API_LINT, "L1");
    public static final Issue CONFIG_FIELD_NAME = new Issue(350, Severity.ERROR, Category.API_LINT);
    public static final Issue RESOURCE_FIELD_NAME = new Issue(351, Severity.ERROR, Category.API_LINT);
    public static final Issue RESOURCE_VALUE_FIELD_NAME = new Issue(352, Severity.ERROR, Category.API_LINT, "C7");
    public static final Issue RESOURCE_STYLE_FIELD_NAME = new Issue(353, Severity.ERROR, Category.API_LINT, "C7");
    public static final Issue STREAM_FILES = new Issue(354, Severity.WARNING, Category.API_LINT, "M10");
    public static final Issue PARCELABLE_LIST = new Issue(355, Severity.WARNING, Category.API_LINT);
    public static final Issue ABSTRACT_INNER = new Issue(356, Severity.WARNING, Category.API_LINT);
    public static final Issue BANNED_THROW = new Issue(358, Severity.ERROR, Category.API_LINT);
    public static final Issue EXTENDS_ERROR = new Issue(359, Severity.ERROR, Category.API_LINT);
    public static final Issue EXCEPTION_NAME = new Issue(360, Severity.ERROR, Category.API_LINT);
    public static final Issue METHOD_NAME_UNITS = new Issue(361, Severity.ERROR, Category.API_LINT);
    public static final Issue FRACTION_FLOAT = new Issue(362, Severity.ERROR, Category.API_LINT);
    public static final Issue PERCENTAGE_INT = new Issue(363, Severity.ERROR, Category.API_LINT);
    public static final Issue NOT_CLOSEABLE = new Issue(364, Severity.WARNING, Category.API_LINT);
    public static final Issue KOTLIN_OPERATOR = new Issue(365, Severity.INFO, Category.API_LINT);
    public static final Issue ARRAY_RETURN = new Issue(366, Severity.WARNING, Category.API_LINT);
    public static final Issue USER_HANDLE = new Issue(367, Severity.WARNING, Category.API_LINT);
    public static final Issue USER_HANDLE_NAME = new Issue(368, Severity.WARNING, Category.API_LINT);
    public static final Issue SERVICE_NAME = new Issue(369, Severity.ERROR, Category.API_LINT, "C4");
    public static final Issue METHOD_NAME_TENSE = new Issue(370, Severity.WARNING, Category.API_LINT);
    public static final Issue NO_CLONE = new Issue(371, Severity.ERROR, Category.API_LINT);
    public static final Issue USE_ICU = new Issue(372, Severity.WARNING, Category.API_LINT);
    public static final Issue USE_PARCEL_FILE_DESCRIPTOR = new Issue(373, Severity.ERROR, Category.API_LINT, "FW11");
    public static final Issue NO_BYTE_OR_SHORT = new Issue(374, Severity.WARNING, Category.API_LINT, "FW12");
    public static final Issue SINGLETON_CONSTRUCTOR = new Issue(375, Severity.ERROR, Category.API_LINT);
    public static final Issue COMMON_ARGS_FIRST = new Issue(376, Severity.WARNING, Category.API_LINT, "M2");
    public static final Issue CONSISTENT_ARGUMENT_ORDER = new Issue(377, Severity.ERROR, Category.API_LINT, "M2");
    public static final Issue KOTLIN_KEYWORD = new Issue(378, Severity.ERROR, Category.API_LINT); // Formerly 141
    public static final Issue UNIQUE_KOTLIN_OPERATOR = new Issue(379, Severity.ERROR, Category.API_LINT);
    public static final Issue SAM_SHOULD_BE_LAST = new Issue(380, Severity.WARNING, Category.API_LINT); // Formerly 142
    public static final Issue MISSING_JVMSTATIC = new Issue(381, Severity.WARNING, Category.API_LINT); // Formerly 143
    public static final Issue DEFAULT_VALUE_CHANGE = new Issue(382, Severity.ERROR, Category.API_LINT); // Formerly 144
    public static final Issue DOCUMENT_EXCEPTIONS = new Issue(383, Severity.ERROR, Category.API_LINT); // Formerly 145
    public static final Issue FORBIDDEN_SUPER_CLASS = new Issue(384, Severity.ERROR, Category.API_LINT);
    public static final Issue MISSING_NULLABILITY = new Issue(385, Severity.ERROR, Category.API_LINT);
    public static final Issue MUTABLE_BARE_FIELD = new Issue(386, Severity.ERROR, Category.API_LINT, "F2");
    public static final Issue INTERNAL_FIELD = new Issue(387, Severity.ERROR, Category.API_LINT, "F2");
    public static final Issue PUBLIC_TYPEDEF = new Issue(388, Severity.ERROR, Category.API_LINT, "FW15");
    public static final Issue ANDROID_URI = new Issue(389, Severity.ERROR, Category.API_LINT, "FW14");

    static {
        // Attempt to initialize issue names based on the field names
        try {
            for (Field field : Issues.class.getDeclaredFields()) {
                Object o = field.get(null);
                if (o instanceof Issue) {
                    Issue issue = (Issue) o;
                    String fieldName = field.getName();
                    issue.fieldName = fieldName;
                    issue.name = underlinesToCamelCase(fieldName.toLowerCase(Locale.US));
                    nameToIssue.put(issue.name, issue);
                    idToIssue.put(issue.code, issue);
                }
            }
        } catch (Throwable unexpected) {
            unexpected.printStackTrace();
        }
    }

    @Nullable
    public static Issue findIssueById(int id) {
        return idToIssue.get(id);
    }

    @Nullable
    public static Issue findIssueById(String id) {
        return nameToIssue.get(id);
    }

    public static boolean setIssueLevel(String id, Severity level, boolean setByUser) {
        if (id.contains(",")) { // Handle being passed in multiple comma separated id's
            boolean ok = true;
            for (String individualId : Splitter.on(',').trimResults().split(id)) {
                ok = setIssueLevel(individualId, level, setByUser) && ok;
            }
            return ok;
        }
        int code = -1;
        if (Character.isDigit(id.charAt(0))) {
            code = Integer.parseInt(id);
        }

        Issue issue = nameToIssue.get(id);
        if (issue == null) {
            try {
                int n = Integer.parseInt(id);
                issue = idToIssue.get(n);
            } catch (NumberFormatException ignore) {
            }
        }

        if (issue == null) {
            for (Issue e : ISSUES) {
                if (e.code == code || id.equalsIgnoreCase(e.name)) {
                    issue = e;
                    break;
                }
            }
        }

        if (issue != null) {
            issue.setLevel(level);
            issue.setByUser = setByUser;
            return true;
        }
        return false;
    }

    // Primary needed by unit tests; ensure that a previous test doesn't influence
    // a later one
    public static void resetLevels() {
        for (Issue issue : ISSUES) {
            issue.level = issue.defaultLevel;
        }
    }
}

