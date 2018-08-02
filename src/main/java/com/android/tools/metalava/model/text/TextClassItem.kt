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

package com.android.tools.metalava.model.text

import com.android.tools.metalava.doclava1.SourcePositionInfo
import com.android.tools.metalava.doclava1.TextCodebase
import com.android.tools.metalava.model.AnnotationRetention
import com.android.tools.metalava.model.ClassItem
import com.android.tools.metalava.model.ConstructorItem
import com.android.tools.metalava.model.FieldItem
import com.android.tools.metalava.model.Item
import com.android.tools.metalava.model.MethodItem
import com.android.tools.metalava.model.PackageItem
import com.android.tools.metalava.model.PropertyItem
import com.android.tools.metalava.model.TypeItem
import com.android.tools.metalava.model.TypeParameterList
import com.android.tools.metalava.model.TypeParameterListOwner
import java.util.function.Predicate

open class TextClassItem(
    override val codebase: TextCodebase,
    position: SourcePositionInfo = SourcePositionInfo.UNKNOWN,
    isPublic: Boolean = false,
    isProtected: Boolean = false,
    isPrivate: Boolean = false,
    isInternal: Boolean = false,
    isStatic: Boolean = false,
    private var isInterface: Boolean = false,
    isAbstract: Boolean = false,
    private var isEnum: Boolean = false,
    private var isAnnotation: Boolean = false,
    isFinal: Boolean = false,
    isSealed: Boolean = false,
    val qualifiedName: String = "",
    private val qualifiedTypeName: String = qualifiedName,
    var name: String = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1),
    val annotations: List<String>? = null
) : TextItem(
    codebase = codebase,
    position = position,
    modifiers = TextModifiers(
        codebase = codebase,
        annotationStrings = annotations,
        public = isPublic, protected = isProtected, private = isPrivate, internal = isInternal,
        static = isStatic, abstract = isAbstract, final = isFinal, sealed = isSealed
    )
), ClassItem, TypeParameterListOwner {
    init {
        @Suppress("LeakingThis")
        (modifiers as TextModifiers).owner = this
    }

    override val isTypeParameter: Boolean = false

    override var artifact: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClassItem) return false

        return qualifiedName == other.qualifiedName()
    }

    override fun hashCode(): Int {
        return qualifiedName.hashCode()
    }

    override fun interfaceTypes(): List<TypeItem> = interfaceTypes
    override fun allInterfaces(): Sequence<ClassItem> {
        return interfaceTypes.asSequence().map { it.asClass() }.filterNotNull()
    }

    private var innerClasses: List<ClassItem> = mutableListOf()

    override var defaultConstructor: ConstructorItem? = null

    override var hasPrivateConstructor: Boolean = false

    override fun innerClasses(): List<ClassItem> = innerClasses

    override fun hasImplicitDefaultConstructor(): Boolean {
        return false
    }

    override fun isInterface(): Boolean = isInterface
    override fun isAnnotationType(): Boolean = isAnnotation
    override fun isEnum(): Boolean = isEnum

    var containingClass: TextClassItem? = null
    override fun containingClass(): ClassItem? = containingClass

    private var containingPackage: PackageItem? = null

    fun setContainingPackage(containingPackage: TextPackageItem) {
        this.containingPackage = containingPackage
    }

    fun setIsAnnotationType(isAnnotation: Boolean) {
        this.isAnnotation = isAnnotation
    }

    fun setIsEnum(isEnum: Boolean) {
        this.isEnum = isEnum
    }

    override fun containingPackage(): PackageItem = containingPackage ?: error(this)

    override fun toType(): TypeItem = codebase.obtainTypeFromString(
        if (typeParameterList().toString().isNotEmpty())
// TODO: No, handle List<String>[], though this is highly unlikely in a class
            qualifiedName() + "<" + typeParameterList() + ">"
        else qualifiedName()
    )

    override fun hasTypeVariables(): Boolean {
        return typeInfo?.hasTypeArguments() ?: false
    }

    private var typeParameterList: TypeParameterList? = null

    override fun typeParameterList(): TypeParameterList {
        if (typeParameterList == null) {
            val s = typeInfo.toString()
            // TODO: No, handle List<String>[]  (though it's not likely for type parameters)
            val index = s.indexOf('<')
            if (index != -1) {
                typeParameterList = TextTypeParameterList.create(codebase, this, s.substring(index))
            } else {
                typeParameterList = TypeParameterList.NONE
            }
        }

        return typeParameterList!!
    }

    private var superClass: ClassItem? = null
    private var superClassType: TypeItem? = null

    override fun superClass(): ClassItem? = superClass
    override fun superClassType(): TypeItem? = superClassType

    override fun setSuperClass(superClass: ClassItem?, superClassType: TypeItem?) {
        this.superClass = superClass
        this.superClassType = superClassType
    }

    override fun setInterfaceTypes(interfaceTypes: List<TypeItem>) {
        this.interfaceTypes = interfaceTypes.toMutableList()
    }

    private var typeInfo: TextTypeItem? = null
    fun setTypeInfo(typeInfo: TextTypeItem) {
        this.typeInfo = typeInfo
    }

    fun asTypeInfo(): TextTypeItem {
        if (typeInfo == null) {
            typeInfo = codebase.obtainTypeFromString(qualifiedTypeName)
        }
        return typeInfo!!
    }

    private var interfaceTypes = mutableListOf<TypeItem>()
    private val constructors = mutableListOf<ConstructorItem>()
    private val methods = mutableListOf<MethodItem>()
    private val fields = mutableListOf<FieldItem>()
    private val properties = mutableListOf<PropertyItem>()

    override fun constructors(): List<ConstructorItem> = constructors
    override fun methods(): List<MethodItem> = methods
    override fun fields(): List<FieldItem> = fields
    override fun properties(): List<PropertyItem> = properties

    fun addInterface(itf: TypeItem) {
        interfaceTypes.add(itf)
    }

    fun addInterface(itf: TextClassItem) {
        interfaceTypes.add(itf.toType())
    }

    fun addConstructor(constructor: TextConstructorItem) {
        constructors += constructor
    }

    fun addMethod(method: TextMethodItem) {
        methods += method
    }

    fun addField(field: TextFieldItem) {
        fields += field
    }

    fun addProperty(property: TextPropertyItem) {
        properties += property
    }

    fun addEnumConstant(field: TextFieldItem) {
        field.setEnumConstant(true)
        fields += field
    }

    fun addInnerClass(cls: TextClassItem) {
        innerClasses += cls
    }

    override fun filteredSuperClassType(predicate: Predicate<Item>): TypeItem? {
        // No filtering in signature files: we assume signature APIs
        // have already been filtered and all items should match.
        // This lets us load signature files and rewrite them using updated
        // output formats etc.
        return superClassType
    }

    private var retention: AnnotationRetention? = null

    override fun getRetention(): AnnotationRetention {
        retention?.let { return it }

        if (!isAnnotationType()) {
            error("getRetention() should only be called on annotation classes")
        }

        retention = ClassItem.findRetention(this)
        return retention!!
    }

    private var fullName: String = name
    override fun simpleName(): String = name.substring(name.lastIndexOf('.') + 1)
    override fun fullName(): String = fullName
    override fun qualifiedName(): String = qualifiedName
    override fun toString(): String = qualifiedName()

    companion object {
        fun createClassStub(codebase: TextCodebase, name: String): TextClassItem =
            createStub(codebase, name, isInterface = false)

        fun createInterfaceStub(codebase: TextCodebase, name: String): TextClassItem =
            createStub(codebase, name, isInterface = true)

        fun createStub(codebase: TextCodebase, name: String, isInterface: Boolean): TextClassItem {
            val index = if (name.endsWith(">")) name.indexOf('<') else -1
            val qualifiedName = if (index == -1) name else name.substring(0, index)
            val cls = TextClassItem(
                codebase = codebase,
                qualifiedName = qualifiedName,
                isInterface = isInterface,
                isPublic = true
            )

            addStubPackage(name, codebase, cls)

            if (index != -1) {
                cls.typeParameterList = TextTypeParameterList.create(codebase, cls, name.substring(index))
            }

            return cls
        }

        private fun addStubPackage(
            name: String,
            codebase: TextCodebase,
            textClassItem: TextClassItem
        ) {
            val endIndex = name.lastIndexOf('.')
            val pkgPath = name.substring(0, endIndex)
            val pkg = codebase.findPackage(pkgPath) as? TextPackageItem ?: TextPackageItem(
                codebase,
                pkgPath,
                SourcePositionInfo.UNKNOWN
            )
            textClassItem.setContainingPackage(pkg)
        }
    }
}
