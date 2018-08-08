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

import com.android.tools.lint.detector.api.ConstantEvaluator
import com.android.tools.metalava.doclava1.Errors
import com.android.tools.metalava.model.Item
import com.android.tools.metalava.model.canonicalizeFloatingPointString
import com.android.tools.metalava.model.javaEscapeString
import com.android.tools.metalava.reporter
import com.android.utils.XmlUtils
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiTypeCastExpression
import com.intellij.psi.PsiVariable
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UBinaryExpressionWithType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UUnaryExpression
import org.jetbrains.uast.util.isArrayInitializer
import org.jetbrains.uast.util.isTypeCast

fun constantToSource(value: Any?): String {
    if (value == null) {
        return "null"
    }

    when (value) {
        is Int -> {
            return value.toString()
        }
        is String -> {
            return "\"${javaEscapeString(value)}\""
        }
        is Long -> {
            return value.toString() + "L"
        }
        is Boolean -> {
            return value.toString()
        }
        is Byte -> {
            return Integer.toHexString(value.toInt())
        }
        is Short -> {
            return Integer.toHexString(value.toInt())
        }
        is Float -> {
            return when (value) {
                Float.POSITIVE_INFINITY -> "(1.0f/0.0f)"
                Float.NEGATIVE_INFINITY -> "(-1.0f/0.0f)"
                Float.NaN -> "(0.0f/0.0f)"
                else -> {
                    canonicalizeFloatingPointString(value.toString()) + "f"
                }
            }
        }
        is Double -> {
            return when (value) {
                Double.POSITIVE_INFINITY -> "(1.0/0.0)"
                Double.NEGATIVE_INFINITY -> "(-1.0/0.0)"
                Double.NaN -> "(0.0/0.0)"
                else -> {
                    canonicalizeFloatingPointString(value.toString())
                }
            }
        }
        is Char -> {
            return String.format("'%s'", javaEscapeString(value.toString()))
        }
    }

    return value.toString()
}

fun constantToExpression(constant: Any?): String? {
    return when (constant) {
        is Int -> "0x${Integer.toHexString(constant)}"
        is String -> "\"${javaEscapeString(constant)}\""
        is Long -> "${constant}L"
        is Boolean -> constant.toString()
        is Byte -> Integer.toHexString(constant.toInt())
        is Short -> Integer.toHexString(constant.toInt())
        is Float -> {
            when (constant) {
                Float.POSITIVE_INFINITY -> "Float.POSITIVE_INFINITY"
                Float.NEGATIVE_INFINITY -> "Float.NEGATIVE_INFINITY"
                Float.NaN -> "Float.NaN"
                else -> {
                    "${canonicalizeFloatingPointString(constant.toString())}F"
                }
            }
        }
        is Double -> {
            when (constant) {
                Double.POSITIVE_INFINITY -> "Double.POSITIVE_INFINITY"
                Double.NEGATIVE_INFINITY -> "Double.NEGATIVE_INFINITY"
                Double.NaN -> "Double.NaN"
                else -> {
                    canonicalizeFloatingPointString(constant.toString())
                }
            }
        }
        is Char -> {
            "'${javaEscapeString(constant.toString())}'"
        }
        else -> {
            null
        }
    }
}

private fun appendSourceLiteral(v: Any?, sb: StringBuilder, owner: Item): Boolean {
    if (v == null) {
        sb.append("null")
        return true
    }
    when (v) {
        is Int, is Boolean, is Byte, is Short -> {
            sb.append(v.toString())
            return true
        }
        is Long -> {
            sb.append(v.toString()).append('L')
            return true
        }
        is String -> {
            sb.append('"').append(javaEscapeString(v)).append('"')
            return true
        }
        is Float -> {
            return when (v) {
                Float.POSITIVE_INFINITY -> {
                    // This convention (displaying fractions) is inherited from doclava
                    sb.append("(1.0f/0.0f)"); true
                }
                Float.NEGATIVE_INFINITY -> {
                    sb.append("(-1.0f/0.0f)"); true
                }
                Float.NaN -> {
                    sb.append("(0.0f/0.0f)"); true
                }
                else -> {
                    sb.append(canonicalizeFloatingPointString(v.toString()) + "f")
                    true
                }
            }
        }
        is Double -> {
            return when (v) {
                Double.POSITIVE_INFINITY -> {
                    // This convention (displaying fractions) is inherited from doclava
                    sb.append("(1.0/0.0)"); true
                }
                Double.NEGATIVE_INFINITY -> {
                    sb.append("(-1.0/0.0)"); true
                }
                Double.NaN -> {
                    sb.append("(0.0/0.0)"); true
                }
                else -> {
                    sb.append(canonicalizeFloatingPointString(v.toString()))
                    true
                }
            }
        }
        is Char -> {
            sb.append('\'').append(javaEscapeString(v.toString())).append('\'')
            return true
        }
        else -> {
            reporter.report(Errors.INTERNAL_ERROR, owner, "Unexpected literal value $v")
        }
    }

    return false
}

/** Given an annotation member value, returns the corresponding Java source expression */
fun toSourceExpression(value: PsiAnnotationMemberValue, owner: Item): String {
    val sb = StringBuilder()
    appendSourceExpression(value, sb, owner)
    return sb.toString()
}

private fun appendSourceExpression(value: PsiAnnotationMemberValue, sb: StringBuilder, owner: Item): Boolean {
    if (value is PsiReference) {
        val resolved = value.resolve()
        if (resolved is PsiField) {
            sb.append(resolved.containingClass?.qualifiedName).append('.').append(resolved.name)
            return true
        }
    } else if (value is PsiLiteral) {
        return appendSourceLiteral(value.value, sb, owner)
    } else if (value is PsiClassObjectAccessExpression) {
        sb.append(value.operand.type.canonicalText).append(".class")
        return true
    } else if (value is PsiArrayInitializerMemberValue) {
        sb.append('{')
        var first = true
        val initialLength = sb.length
        for (e in value.initializers) {
            val length = sb.length
            if (first) {
                first = false
            } else {
                sb.append(", ")
            }
            val appended = appendSourceExpression(e, sb, owner)
            if (!appended) {
                // trunk off comma if it bailed for some reason (e.g. constant
                // filtered out by API etc)
                sb.setLength(length)
                if (length == initialLength) {
                    first = true
                }
            }
        }
        sb.append('}')
        return true
    } else if (value is PsiAnnotation) {
        sb.append('@').append(value.qualifiedName)
        return true
    } else {
        if (value is PsiTypeCastExpression) {
            val type = value.castType?.type
            val operand = value.operand
            if (type != null && operand is PsiAnnotationMemberValue) {
                sb.append('(')
                sb.append(type.canonicalText)
                sb.append(')')
                return appendSourceExpression(operand, sb, owner)
            }
        }
        val constant = ConstantEvaluator.evaluate(null, value)
        if (constant != null) {
            return appendSourceLiteral(constant, sb, owner)
        }
    }
    reporter.report(Errors.INTERNAL_ERROR, owner, "Unexpected annotation default value $value")
    return false
}

fun toSourceString(
    value: UExpression?,
    inlineFieldValues: Boolean,
    inlineConstants: Boolean = true,
    skipUnknown: Boolean = false,
    filterFields: ((String, String) -> Boolean)? = null,
    warning: (String) -> Unit = {}
): String? {
    value ?: return null
    val sb = StringBuilder()
    return if (appendExpression(sb, value, inlineFieldValues, inlineConstants, skipUnknown, filterFields, warning)
    ) {
        sb.toString()
    } else {
        null
    }
}

private fun appendExpression(
    sb: StringBuilder,
    expression: UExpression,
    inlineFieldValues: Boolean,
    inlineConstants: Boolean,
    skipUnknown: Boolean,
    filterFields: ((String, String) -> Boolean)?,
    warning: (String) -> Unit = {}
): Boolean {
    if (expression.isArrayInitializer()) {
        val call = expression as UCallExpression
        val initializers = call.valueArguments
        sb.append('{')
        var first = true
        val initialLength = sb.length
        for (e in initializers) {
            val length = sb.length
            if (first) {
                first = false
            } else {
                sb.append(", ")
            }
            val appended = appendExpression(
                sb,
                e,
                inlineFieldValues,
                inlineConstants,
                skipUnknown,
                filterFields,
                warning
            )
            if (!appended) {
                // truncate trailing comma if it bailed for some reason (e.g. constant
                // filtered out by API etc)
                sb.setLength(length)
                if (length == initialLength) {
                    first = true
                }
            }
        }
        sb.append('}')
        return sb.length != 2
    } else if (expression is UReferenceExpression) {
        val resolved = expression.resolve()
        if (resolved is PsiField) {
            val field = resolved
            if (!inlineFieldValues) {
                val value = field.computeConstantValue()
                if (appendLiteralValue(sb, value)) {
                    return true
                }
            }

            val declaringClass = field.containingClass
            if (declaringClass == null) {
                warning("No containing class found for " + field.name)
                return false
            }
            val qualifiedName = declaringClass.qualifiedName
            val fieldName = field.name

            if (qualifiedName != null) {
                if (filterFields != null && !filterFields(fieldName, qualifiedName)) {
                    return false
                }
                sb.append(qualifiedName)
                sb.append('.')
                sb.append(fieldName)
                return true
            }
            if (skipUnknown) {
                return false
            } else {
                sb.append(expression.asSourceString())
                return true
            }
        } else if (resolved is PsiVariable) {
            sb.append(resolved.name)
            return true
        } else {
            if (skipUnknown) {
                warning("Unexpected reference to $expression")
                return false
            }
            sb.append(expression.asSourceString())
            return true
        }
    } else if (expression is ULiteralExpression) {
        val literalValue = expression.value
        if (appendLiteralValue(sb, literalValue)) {
            return true
        }
    } else if (expression is UAnnotation) {
        sb.append('@').append(expression.qualifiedName)
        return true
    } else if (expression is UBinaryExpressionWithType) {
        if ((expression).isTypeCast()) {
            val operand = expression.operand
            return appendExpression(sb, operand, inlineFieldValues, inlineConstants, skipUnknown, filterFields, warning)
        }
        return false
    } else if (expression is UBinaryExpression) {
        if (inlineConstants) {
            val constant = expression.evaluate()
            if (constant != null) {
                sb.append(constantToSource(constant))
                return true
            }
        }

        if (appendExpression(
                sb,
                expression.leftOperand,
                inlineFieldValues,
                inlineConstants,
                skipUnknown,
                filterFields,
                warning
            )) {
            sb.append(' ').append(expression.operator.text).append(' ')
            if (appendExpression(
                    sb,
                    expression.rightOperand,
                    inlineFieldValues,
                    inlineConstants,
                    skipUnknown,
                    filterFields,
                    warning
                )) {
                return true
            }
        }
    } else if (expression is UUnaryExpression) {
        sb.append(expression.operator.text)
        if (appendExpression(
                sb,
                expression.operand,
                inlineFieldValues,
                inlineConstants,
                skipUnknown,
                filterFields,
                warning
            )) {
            return true
        }
    } else {
        sb.append(expression.asSourceString())
        return true
    }

    // For example, binary expressions like 3 + 4
    val literalValue = ConstantEvaluator.evaluate(null, expression)
    if (literalValue != null) {
        if (appendLiteralValue(sb, literalValue)) {
            return true
        }
    }

    warning("Unexpected annotation expression of type ${expression.javaClass} and is $expression")

    return false
}

private fun appendLiteralValue(sb: StringBuilder, literalValue: Any?): Boolean {
    if (literalValue is Number || literalValue is Boolean) {
        sb.append(literalValue.toString())
        return true
    } else if (literalValue is String || literalValue is Char) {
        sb.append('"')
        XmlUtils.appendXmlAttributeValue(sb, literalValue.toString())
        sb.append('"')
        return true
    }
    return false
}
