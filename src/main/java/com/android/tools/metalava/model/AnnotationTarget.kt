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

/** Various places where a given annotation can be written */
enum class AnnotationTarget {
    /** Write the annotation into the signature file */
    SIGNATURE_FILE,
    /** Write the annotation into stub source files */
    STUBS_FILE,
    /** Write the annotation into external annotation files */
    EXTERNAL_ANNOTATIONS_FILE,
    /** Don't write the annotation anywhere */
    NONE;
}

/** Don't write this annotation anywhere; it is not API significant. */
val NO_ANNOTATION_TARGETS = setOf(AnnotationTarget.NONE)

/**
 * Annotation is API significant: write it into the signature file and stub source code.
 * This would normally be the case for all (API significant) class-retention annotations,
 * but unfortunately due to apt (the annotation proessor) attempting to load all
 * classes for annotation references that it comes across, that means we cannot
 * compile the stubs with the androidx annotations and leave those in the SDK; apt
 * would also need to have androidx on the classpath. So instead we put all these
 * annotations (except for @RecentlyNullable and @RecentlyNonNull, which are not part
 * of androidx, and which we include as package private in the SDK, something we cannot
 * do with the others since their class definitions conflict with the real androidx library
 * when present) into the external annotations file.
 *
 */
val ANNOTATION_IN_STUBS = setOf(AnnotationTarget.SIGNATURE_FILE, AnnotationTarget.STUBS_FILE)

/** Annotation is API significant: write it into the signature file and into external annotations file. */
val ANNOTATION_EXTERNAL = setOf(AnnotationTarget.SIGNATURE_FILE, AnnotationTarget.EXTERNAL_ANNOTATIONS_FILE)

/** Write it only into the external annotations file, not the signature file */
val ANNOTATION_EXTERNAL_ONLY = setOf(AnnotationTarget.SIGNATURE_FILE, AnnotationTarget.EXTERNAL_ANNOTATIONS_FILE)
