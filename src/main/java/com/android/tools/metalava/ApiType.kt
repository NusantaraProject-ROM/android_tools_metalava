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

import com.android.tools.metalava.doclava1.ApiPredicate
import com.android.tools.metalava.doclava1.ElidingPredicate
import com.android.tools.metalava.doclava1.FilterPredicate
import com.android.tools.metalava.model.Item
import java.util.function.Predicate

/** Types of APIs emitted (or parsed etc) */
enum class ApiType(val displayName: String) {
    /** The public API */
    PUBLIC_API("api"),
    /** The API that has been removed */
    REMOVED("removed"),
    /** The private API */
    PRIVATE("private");

    fun getEmitFilter(): Predicate<Item> {
        return when {
            this == PUBLIC_API -> {
                val apiFilter = FilterPredicate(ApiPredicate())
                val apiReference = ApiPredicate(ignoreShown = true)
                apiFilter.and(ElidingPredicate(apiReference))
            }
            this == REMOVED -> {
                val removedFilter = FilterPredicate(ApiPredicate(matchRemoved = true))
                val removedReference = ApiPredicate(ignoreShown = true, ignoreRemoved = true)
                removedFilter.and(ElidingPredicate(removedReference))
            }
            else -> {
                val apiFilter = FilterPredicate(ApiPredicate())
                val memberIsNotCloned: Predicate<Item> = Predicate { !it.isCloned() }
                memberIsNotCloned.and(apiFilter.negate())
            }
        }
    }

    fun getReferenceFilter(): Predicate<Item> {
        return when {
            this == PUBLIC_API -> ApiPredicate(ignoreShown = true)
            this == REMOVED -> ApiPredicate(ignoreShown = true, ignoreRemoved = true)
            else -> Predicate { true }
        }
    }
}
