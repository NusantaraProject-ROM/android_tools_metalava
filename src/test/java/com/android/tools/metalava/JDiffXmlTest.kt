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

import org.junit.Test

class JDiffXmlTest : DriverTest() {

    @Test
    fun `Loading a signature file and writing the API back out`() {
        check(
            compatibilityMode = true,
            signatureSource =
            """
            package test.pkg {
              public deprecated class MyTest {
                ctor public MyTest();
                method public deprecated int clamp(int);
                method public java.lang.Double convert(java.lang.Float);
                field public static final java.lang.String ANY_CURSOR_ITEM_TYPE = "vnd.android.cursor.item/*";
                field public deprecated java.lang.Number myNumber;
              }
            }
            """,
            apiXml =
            """
            <api>
            <package name="test.pkg"
            >
            <class name="MyTest"
             extends="java.lang.Object"
             abstract="false"
             static="false"
             final="false"
             deprecated="deprecated"
             visibility="public"
            >
            <constructor name="MyTest"
             type="test.pkg.MyTest"
             static="false"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            </constructor>
            <method name="clamp"
             return="int"
             abstract="false"
             native="false"
             synchronized="false"
             static="false"
             final="false"
             deprecated="deprecated"
             visibility="public"
            >
            <parameter name="null" type="int">
            </parameter>
            </method>
            <method name="convert"
             return="java.lang.Double"
             abstract="false"
             native="false"
             synchronized="false"
             static="false"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            <parameter name="null" type="java.lang.Float">
            </parameter>
            </method>
            <field name="ANY_CURSOR_ITEM_TYPE"
             type="java.lang.String"
             transient="false"
             volatile="false"
             value="&quot;vnd.android.cursor.item/*&quot;"
             static="true"
             final="true"
             deprecated="not deprecated"
             visibility="public"
            >
            </field>
            <field name="myNumber"
             type="java.lang.Number"
             transient="false"
             volatile="false"
             static="false"
             final="false"
             deprecated="deprecated"
             visibility="public"
            >
            </field>
            </class>
            </package>
            </api>
            """
        )
    }

    @Test
    fun `Test generics, superclasses and interfaces`() {
        val source = """
            package a.b.c {
              public abstract interface MyStream<T, S extends a.b.c.MyStream<T, S>> {
              }
            }
            package test.pkg {
              public final class Foo extends java.lang.Enum {
                ctor public Foo(int);
                ctor public Foo(int, int);
                method public static test.pkg.Foo valueOf(java.lang.String);
                method public static final test.pkg.Foo[] values();
              }
              public abstract interface MyBaseInterface {
              }
              public abstract interface MyInterface<T> implements test.pkg.MyBaseInterface {
              }
              public abstract interface MyInterface2<T extends java.lang.Number> implements test.pkg.MyBaseInterface {
              }
              public static abstract class MyInterface2.Range<T extends java.lang.Comparable<? super T>> {
                ctor public MyInterface2.Range();
              }
              public static class MyInterface2.TtsSpan<C extends test.pkg.MyInterface<?>> {
                ctor public MyInterface2.TtsSpan();
              }
              public final class Test<T> {
                ctor public Test();
              }
            }
            """
        check(
            compatibilityMode = true,
            signatureSource = source,
            checkDoclava1 = true,
            apiXml =
            """
            <api>
            <package name="a.b.c"
            >
            <interface name="MyStream"
             abstract="true"
             static="false"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            </interface>
            </package>
            <package name="test.pkg"
            >
            <class name="Foo"
             extends="java.lang.Enum"
             abstract="false"
             static="false"
             final="true"
             deprecated="not deprecated"
             visibility="public"
            >
            <constructor name="Foo"
             type="test.pkg.Foo"
             static="false"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            <parameter name="null" type="int">
            </parameter>
            </constructor>
            <constructor name="Foo"
             type="test.pkg.Foo"
             static="false"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            <parameter name="null" type="int">
            </parameter>
            <parameter name="null" type="int">
            </parameter>
            </constructor>
            <method name="valueOf"
             return="test.pkg.Foo"
             abstract="false"
             native="false"
             synchronized="false"
             static="true"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            <parameter name="null" type="java.lang.String">
            </parameter>
            </method>
            <method name="values"
             return="test.pkg.Foo[]"
             abstract="false"
             native="false"
             synchronized="false"
             static="true"
             final="true"
             deprecated="not deprecated"
             visibility="public"
            >
            </method>
            </class>
            <interface name="MyBaseInterface"
             abstract="true"
             static="false"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            </interface>
            <interface name="MyInterface"
             abstract="true"
             static="false"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            <implements name="test.pkg.MyBaseInterface">
            </implements>
            </interface>
            <interface name="MyInterface2"
             abstract="true"
             static="false"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            <implements name="test.pkg.MyBaseInterface">
            </implements>
            </interface>
            <class name="MyInterface2.Range"
             extends="java.lang.Object"
             abstract="true"
             static="true"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            <constructor name="MyInterface2.Range"
             type="test.pkg.MyInterface2.Range"
             static="false"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            </constructor>
            </class>
            <class name="MyInterface2.TtsSpan"
             extends="java.lang.Object"
             abstract="false"
             static="true"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            <constructor name="MyInterface2.TtsSpan"
             type="test.pkg.MyInterface2.TtsSpan"
             static="false"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            </constructor>
            </class>
            <class name="Test"
             extends="java.lang.Object"
             abstract="false"
             static="false"
             final="true"
             deprecated="not deprecated"
             visibility="public"
            >
            <constructor name="Test"
             type="test.pkg.Test"
             static="false"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            </constructor>
            </class>
            </package>
            </api>
            """
        )
    }

    @Test
    fun `Test enums`() {
        val source = """
            package test.pkg {
              public final class Foo extends java.lang.Enum {
                ctor public Foo(int);
                ctor public Foo(int, int);
                method public static test.pkg.Foo valueOf(java.lang.String);
                method public static final test.pkg.Foo[] values();
                enum_constant public static final test.pkg.Foo A;
                enum_constant public static final test.pkg.Foo B;
              }
            }
            """
        check(
            compatibilityMode = false,
            signatureSource = source,
            checkDoclava1 = false, // because doclava1 does not include enum fields; see compat mode below
            apiXml =
            """
            <api>
            <package name="test.pkg"
            >
            <class name="Foo"
             extends="java.lang.Enum"
             abstract="false"
             static="false"
             final="true"
             deprecated="not deprecated"
             visibility="public"
            >
            <constructor name="Foo"
             type="test.pkg.Foo"
             static="false"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            <parameter name="null" type="int">
            </parameter>
            </constructor>
            <constructor name="Foo"
             type="test.pkg.Foo"
             static="false"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            <parameter name="null" type="int">
            </parameter>
            <parameter name="null" type="int">
            </parameter>
            </constructor>
            <method name="valueOf"
             return="test.pkg.Foo"
             abstract="false"
             native="false"
             synchronized="false"
             static="true"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            <parameter name="null" type="java.lang.String">
            </parameter>
            </method>
            <method name="values"
             return="test.pkg.Foo[]"
             abstract="false"
             native="false"
             synchronized="false"
             static="true"
             final="true"
             deprecated="not deprecated"
             visibility="public"
            >
            </method>
            <field name="A"
             type="test.pkg.Foo"
             transient="false"
             volatile="false"
             static="true"
             final="true"
             deprecated="not deprecated"
             visibility="public"
            >
            </field>
            <field name="B"
             type="test.pkg.Foo"
             transient="false"
             volatile="false"
             static="true"
             final="true"
             deprecated="not deprecated"
             visibility="public"
            >
            </field>
            </class>
            </package>
            </api>
            """
        )
    }

    @Test
    fun `Test enums compat mode`() {
        val source = """
            package test.pkg {
              public final class Foo extends java.lang.Enum {
                ctor public Foo(int);
                ctor public Foo(int, int);
                method public static test.pkg.Foo valueOf(java.lang.String);
                method public static final test.pkg.Foo[] values();
                enum_constant public static final test.pkg.Foo A;
                enum_constant public static final test.pkg.Foo B;
              }
            }
            """
        check(
            compatibilityMode = true,
            signatureSource = source,
            checkDoclava1 = true,
            apiXml =
            """
            <api>
            <package name="test.pkg"
            >
            <class name="Foo"
             extends="java.lang.Enum"
             abstract="false"
             static="false"
             final="true"
             deprecated="not deprecated"
             visibility="public"
            >
            <constructor name="Foo"
             type="test.pkg.Foo"
             static="false"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            <parameter name="null" type="int">
            </parameter>
            </constructor>
            <constructor name="Foo"
             type="test.pkg.Foo"
             static="false"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            <parameter name="null" type="int">
            </parameter>
            <parameter name="null" type="int">
            </parameter>
            </constructor>
            <method name="valueOf"
             return="test.pkg.Foo"
             abstract="false"
             native="false"
             synchronized="false"
             static="true"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            <parameter name="null" type="java.lang.String">
            </parameter>
            </method>
            <method name="values"
             return="test.pkg.Foo[]"
             abstract="false"
             native="false"
             synchronized="false"
             static="true"
             final="true"
             deprecated="not deprecated"
             visibility="public"
            >
            </method>
            </class>
            </package>
            </api>
            """
        )
    }

    @Test
    fun `Throws Lists`() {
        check(
            compatibilityMode = true,
            signatureSource = """
                    package android.accounts {
                      public abstract interface AccountManagerFuture<V> {
                        method public abstract V getResult() throws android.accounts.OperationCanceledException, java.io.IOException, android.accounts.AuthenticatorException;
                        method public abstract V getResult(long, java.util.concurrent.TimeUnit) throws android.accounts.OperationCanceledException, java.io.IOException, android.accounts.AuthenticatorException;
                      }
                    }
                    """,
            apiXml =
            """
            <api>
            <package name="android.accounts"
            >
            <interface name="AccountManagerFuture"
             abstract="true"
             static="false"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            <method name="getResult"
             return="V"
             abstract="true"
             native="false"
             synchronized="false"
             static="false"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            <exception name="AuthenticatorException" type="android.accounts.AuthenticatorException">
            </exception>
            <exception name="IOException" type="java.io.IOException">
            </exception>
            <exception name="OperationCanceledException" type="android.accounts.OperationCanceledException">
            </exception>
            </method>
            <method name="getResult"
             return="V"
             abstract="true"
             native="false"
             synchronized="false"
             static="false"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            <parameter name="null" type="long">
            </parameter>
            <parameter name="null" type="java.util.concurrent.TimeUnit">
            </parameter>
            <exception name="AuthenticatorException" type="android.accounts.AuthenticatorException">
            </exception>
            <exception name="IOException" type="java.io.IOException">
            </exception>
            <exception name="OperationCanceledException" type="android.accounts.OperationCanceledException">
            </exception>
            </method>
            </interface>
            </package>
            </api>
            """
        )
    }

    @Test
    fun `Test conversion flag`() {
        check(
            compatibilityMode = true,
            convertToJDiff = listOf(
                ConvertData(
                    """
                    package test.pkg {
                      public class MyTest1 {
                        ctor public MyTest1();
                      }
                    }
                    """,
                    toXml =
                    """
                    <api>
                    <package name="test.pkg"
                    >
                    <class name="MyTest1"
                     extends="java.lang.Object"
                     abstract="false"
                     static="false"
                     final="false"
                     deprecated="not deprecated"
                     visibility="public"
                    >
                    <constructor name="MyTest1"
                     type="test.pkg.MyTest1"
                     static="false"
                     final="false"
                     deprecated="not deprecated"
                     visibility="public"
                    >
                    </constructor>
                    </class>
                    </package>
                    </api>
                    """
                ),
                ConvertData(
                    fromApi =
                    """
                    package test.pkg {
                      public class MyTest2 {
                      }
                    }
                    """,
                    toXml =
                    """
                    <api>
                    <package name="test.pkg"
                    >
                    <class name="MyTest2"
                     extends="java.lang.Object"
                     abstract="false"
                     static="false"
                     final="false"
                     deprecated="not deprecated"
                     visibility="public"
                    >
                    </class>
                    </package>
                    </api>
                    """
                )
            )
        )
    }

    @Test
    fun `Generics in interfaces`() {
        check(
            compatibilityMode = false,
            signatureSource = """
                    package android.accounts {
                      public class ArgbEvaluator implements android.animation.DefaultEvaluator<D> implements android.animation.TypeEvaluator<V> {
                      }
                    }
                    """,
            apiXml =
            """
            <api>
            <package name="android.accounts"
            >
            <class name="ArgbEvaluator"
             extends="java.lang.Object"
             abstract="false"
             static="false"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            <implements name="android.animation.DefaultEvaluator&lt;D>">
            </implements>
            <implements name="android.animation.TypeEvaluator&lt;V>">
            </implements>
            <implements name="java.lang.implements">
            </implements>
            </class>
            </package>
            </api>
            """
        )
    }

    @Test
    fun `Type Parameter Mapping`() {
        check(
            compatibilityMode = false,
            signatureSource = """
                package test.pkg {
                  public interface AbstractList<D,E,F> extends test.pkg.List<A,B,C> {
                  }
                  public interface ConcreteList<G,H,I> extends test.pkg.AbstractList<D,E,F> {
                  }
                  public interface List<A,B,C> {
                  }
                }
                """,
            apiXml =
            """
            <api>
            <package name="test.pkg"
            >
            <interface name="AbstractList"
             extends="test.pkg.List&lt;A,B,C>"
             abstract="false"
             static="false"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            </interface>
            <interface name="ConcreteList"
             extends="test.pkg.AbstractList&lt;D,E,F>"
             abstract="false"
             static="false"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            </interface>
            <interface name="List"
             abstract="false"
             static="false"
             final="false"
             deprecated="not deprecated"
             visibility="public"
            >
            </interface>
            </package>
            </api>
            """
        )
    }

    @Test
    fun `Half float short from signature file`() {
        check(
            compatibilityMode = false,
            signatureSource = """
                package test.pkg {
                  public class Test {
                    ctor public Test();
                    field public static final short LOWEST_VALUE = -1025; // 0xfffffbff
                  }
                }
            """,
            apiXml =
            """
                <api>
                <package name="test.pkg"
                >
                <class name="Test"
                 extends="java.lang.Object"
                 abstract="false"
                 static="false"
                 final="false"
                 deprecated="not deprecated"
                 visibility="public"
                >
                <constructor name="Test"
                 type="test.pkg.Test"
                 static="false"
                 final="false"
                 deprecated="not deprecated"
                 visibility="public"
                >
                </constructor>
                <field name="LOWEST_VALUE"
                 type="short"
                 transient="false"
                 volatile="false"
                 value="-1025"
                 static="true"
                 final="true"
                 deprecated="not deprecated"
                 visibility="public"
                >
                </field>
                </class>
                </package>
                </api>
            """
        )
    }

    @Test
    fun `Half float short from source`() {
        check(
            compatibilityMode = false,
            sourceFiles = *arrayOf(
                java(
                    """
                      package test.pkg;
                      public class Test {
                        public static final short LOWEST_VALUE = (short) 0xfbff;
                      }
                      """
                )
            ),
            apiXml =
            """
                <api>
                <package name="test.pkg"
                >
                <class name="Test"
                 extends="java.lang.Object"
                 abstract="false"
                 static="false"
                 final="false"
                 deprecated="not deprecated"
                 visibility="public"
                >
                <constructor name="Test"
                 type="test.pkg.Test"
                 static="false"
                 final="false"
                 deprecated="not deprecated"
                 visibility="public"
                >
                </constructor>
                <field name="LOWEST_VALUE"
                 type="short"
                 transient="false"
                 volatile="false"
                 value="-1025"
                 static="true"
                 final="true"
                 deprecated="not deprecated"
                 visibility="public"
                >
                </field>
                </class>
                </package>
                </api>
            """
        )
    }

    @Test
    fun `Test convert new with compat mode and api strip`() {
        check(
            compatibilityMode = true,
            convertToJDiff = listOf(
                ConvertData(
                    strip = true,
                    fromApi =
                    """
                    package test.pkg {
                      public class MyTest1 {
                        ctor public MyTest1();
                        method public deprecated int clamp(int);
                        method public java.lang.Double convert(java.lang.Float);
                        field public static final java.lang.String ANY_CURSOR_ITEM_TYPE = "vnd.android.cursor.item/*";
                        field public deprecated java.lang.Number myNumber;
                      }
                      public class MyTest2 {
                        ctor public MyTest2();
                        method public java.lang.Double convert(java.lang.Float);
                      }
                    }
                    package test.pkg.new {
                      public interface MyInterface {
                      }
                      public abstract class MyTest3 implements java.util.List {
                      }
                      public abstract class MyTest4 implements test.pkg.new.MyInterface {
                      }
                    }
                    """,
                    baseApi =
                    """
                    package test.pkg {
                      public class MyTest1 {
                        ctor public MyTest1();
                        method public deprecated int clamp(int);
                        field public deprecated java.lang.Number myNumber;
                      }
                    }
                    """,
                    toXml =
                    """
                    <api>
                    <package name="test.pkg"
                    >
                    <class name="MyTest1"
                     extends="java.lang.Object"
                     abstract="false"
                     static="false"
                     final="false"
                     deprecated="not deprecated"
                     visibility="public"
                    >
                    <method name="convert"
                     return="java.lang.Double"
                     abstract="false"
                     native="false"
                     synchronized="false"
                     static="false"
                     final="false"
                     deprecated="not deprecated"
                     visibility="public"
                    >
                    <parameter name="null" type="java.lang.Float">
                    </parameter>
                    </method>
                    </class>
                    <class name="MyTest2"
                     extends="java.lang.Object"
                     abstract="false"
                     static="false"
                     final="false"
                     deprecated="not deprecated"
                     visibility="public"
                    >
                    <constructor name="MyTest2"
                     type="test.pkg.MyTest2"
                     static="false"
                     final="false"
                     deprecated="not deprecated"
                     visibility="public"
                    >
                    </constructor>
                    <method name="convert"
                     return="java.lang.Double"
                     abstract="false"
                     native="false"
                     synchronized="false"
                     static="false"
                     final="false"
                     deprecated="not deprecated"
                     visibility="public"
                    >
                    <parameter name="null" type="java.lang.Float">
                    </parameter>
                    </method>
                    </class>
                    </package>
                    <package name="test.pkg.new"
                    >
                    <interface name="MyInterface"
                     abstract="false"
                     static="false"
                     final="false"
                     deprecated="not deprecated"
                     visibility="public"
                    >
                    </interface>
                    <class name="MyTest3"
                     extends="java.lang.Object"
                     abstract="true"
                     static="false"
                     final="false"
                     deprecated="not deprecated"
                     visibility="public"
                    >
                    </class>
                    <class name="MyTest4"
                     extends="java.lang.Object"
                     abstract="true"
                     static="false"
                     final="false"
                     deprecated="not deprecated"
                     visibility="public"
                    >
                    <implements name="test.pkg.new.MyInterface">
                    </implements>
                    </class>
                    </package>
                    </api>
                    """
                )
            )
        )
    }

    @Test
    fun `Test convert new without compat mode and no strip`() {
        check(
            compatibilityMode = false,
            convertToJDiff = listOf(
                ConvertData(
                    strip = false,
                    fromApi =
                    """
                    package test.pkg {
                      public class MyTest1 {
                        ctor public MyTest1();
                        method public deprecated int clamp(int);
                        method public java.lang.Double convert(java.lang.Float);
                        field public static final java.lang.String ANY_CURSOR_ITEM_TYPE = "vnd.android.cursor.item/*";
                        field public deprecated java.lang.Number myNumber;
                      }
                      public class MyTest2 {
                        ctor public MyTest2();
                        method public java.lang.Double convert(java.lang.Float);
                      }
                    }
                    package test.pkg.new {
                      public interface MyInterface {
                      }
                      public abstract class MyTest3 implements java.util.List {
                      }
                      public abstract class MyTest4 implements test.pkg.new.MyInterface {
                      }
                    }
                    """,
                    baseApi =
                    """
                    package test.pkg {
                      public class MyTest1 {
                        ctor public MyTest1();
                        method public deprecated int clamp(int);
                        field public deprecated java.lang.Number myNumber;
                      }
                    }
                    """,
                    toXml =
                    """
                    <api>
                    <package name="test.pkg"
                    >
                    <class name="MyTest1"
                     extends="java.lang.Object"
                     abstract="false"
                     static="false"
                     final="false"
                     deprecated="not deprecated"
                     visibility="public"
                    >
                    <method name="convert"
                     return="java.lang.Double"
                     abstract="false"
                     native="false"
                     synchronized="false"
                     static="false"
                     final="false"
                     deprecated="not deprecated"
                     visibility="public"
                    >
                    <parameter name="null" type="java.lang.Float">
                    </parameter>
                    </method>
                    <field name="ANY_CURSOR_ITEM_TYPE"
                     type="java.lang.String"
                     transient="false"
                     volatile="false"
                     value="&quot;vnd.android.cursor.item/*&quot;"
                     static="true"
                     final="true"
                     deprecated="not deprecated"
                     visibility="public"
                    >
                    </field>
                    </class>
                    <class name="MyTest2"
                     extends="java.lang.Object"
                     abstract="false"
                     static="false"
                     final="false"
                     deprecated="not deprecated"
                     visibility="public"
                    >
                    <constructor name="MyTest2"
                     type="test.pkg.MyTest2"
                     static="false"
                     final="false"
                     deprecated="not deprecated"
                     visibility="public"
                    >
                    </constructor>
                    <method name="convert"
                     return="java.lang.Double"
                     abstract="false"
                     native="false"
                     synchronized="false"
                     static="false"
                     final="false"
                     deprecated="not deprecated"
                     visibility="public"
                    >
                    <parameter name="null" type="java.lang.Float">
                    </parameter>
                    </method>
                    </class>
                    </package>
                    <package name="test.pkg.new"
                    >
                    <interface name="MyInterface"
                     abstract="false"
                     static="false"
                     final="false"
                     deprecated="not deprecated"
                     visibility="public"
                    >
                    </interface>
                    <class name="MyTest3"
                     extends="java.lang.Object"
                     abstract="true"
                     static="false"
                     final="false"
                     deprecated="not deprecated"
                     visibility="public"
                    >
                    <implements name="java.util.List">
                    </implements>
                    </class>
                    <class name="MyTest4"
                     extends="java.lang.Object"
                     abstract="true"
                     static="false"
                     final="false"
                     deprecated="not deprecated"
                     visibility="public"
                    >
                    <implements name="test.pkg.new.MyInterface">
                    </implements>
                    </class>
                    </package>
                    </api>
                    """
                )
            )
        )
    }

    @Test
    fun `Test convert nothing new`() {
        check(
            expectedOutput = "No API change detected, not generating diff",
            compatibilityMode = true,
            convertToJDiff = listOf(
                ConvertData(
                    fromApi =
                    """
                    package test.pkg {
                      public class MyTest1 {
                        ctor public MyTest1();
                        method public deprecated int clamp(int);
                        method public java.lang.Double convert(java.lang.Float);
                        field public static final java.lang.String ANY_CURSOR_ITEM_TYPE = "vnd.android.cursor.item/*";
                        field public deprecated java.lang.Number myNumber;
                      }
                      public class MyTest2 {
                        ctor public MyTest2();
                        method public java.lang.Double convert(java.lang.Float);
                      }
                    }
                    package test.pkg.new {
                      public class MyTest3 {
                      }
                    }
                    """,
                    baseApi =
                    """
                    package test.pkg {
                      public class MyTest1 {
                        ctor public MyTest1();
                        method public deprecated int clamp(int);
                        method public java.lang.Double convert(java.lang.Float);
                        field public static final java.lang.String ANY_CURSOR_ITEM_TYPE = "vnd.android.cursor.item/*";
                        field public deprecated java.lang.Number myNumber;
                      }
                      public class MyTest2 {
                        ctor public MyTest2();
                        method public java.lang.Double convert(java.lang.Float);
                      }
                    }
                    package test.pkg.new {
                      public class MyTest3 {
                      }
                    }
                    """,
                    toXml =
                    """
                    """
                )
            )
        )
    }

    @Test
    fun `Test doclava compat`() {
        // A few more differences
        check(
            compatibilityMode = true,
            convertToJDiff = listOf(
                ConvertData(
                    fromApi =
                    """
                    package test.pkg {
                      public class MyTest1 {
                        ctor public MyTest1();
                        method public void method(java.util.List<String>);
                        field protected static final java.lang.String CRLF = "\r\n";
                        field protected static final byte[] CRLF_BYTES;
                      }
                    }
                    """,
                    toXml =
                    """
                    <api>
                    <package name="test.pkg"
                    >
                    <class name="MyTest1"
                     extends="java.lang.Object"
                     abstract="false"
                     static="false"
                     final="false"
                     deprecated="not deprecated"
                     visibility="public"
                    >
                    <constructor name="MyTest1"
                     type="test.pkg.MyTest1"
                     static="false"
                     final="false"
                     deprecated="not deprecated"
                     visibility="public"
                    >
                    </constructor>
                    <method name="method"
                     return="void"
                     abstract="false"
                     native="false"
                     synchronized="false"
                     static="false"
                     final="false"
                     deprecated="not deprecated"
                     visibility="public"
                    >
                    <parameter name="null" type="java.util.List&lt;String&gt;">
                    </parameter>
                    </method>
                    <field name="CRLF"
                     type="java.lang.String"
                     transient="false"
                     volatile="false"
                     value="&quot;\r\n&quot;"
                     static="true"
                     final="true"
                     deprecated="not deprecated"
                     visibility="protected"
                    >
                    </field>
                    <field name="CRLF_BYTES"
                     type="byte[]"
                     transient="false"
                     volatile="false"
                     value="null"
                     static="true"
                     final="true"
                     deprecated="not deprecated"
                     visibility="protected"
                    >
                    </field>
                    </class>
                    </package>
                    </api>
                    """
                )
            )
        )
    }
}