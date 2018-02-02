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

package com.android.tools.metalava

import org.junit.Test

class
CompatibilityCheckTest : DriverTest() {
    @Test
    fun `Change between class and interface`() {
        check(
            checkCompatibility = true,
            warnings = """
                TESTROOT/load-api.txt:2: error: Class test.pkg.MyTest1 changed class/interface declaration [ChangedClass:23]
                TESTROOT/load-api.txt:4: error: Class test.pkg.MyTest2 changed class/interface declaration [ChangedClass:23]
                """,
            compatibilityMode = false,
            previousApi = """
                package test.pkg {
                  public class MyTest1 {
                  }
                  public interface MyTest2 {
                  }
                  public class MyTest3 {
                  }
                  public interface MyTest4 {
                  }
                }
                """,
            // MyTest1 and MyTest2 reversed from class to interface or vice versa, MyTest3 and MyTest4 unchanged
            signatureSource = """
                package test.pkg {
                  public interface MyTest1 {
                  }
                  public class MyTest2 {
                  }
                  public class MyTest3 {
                  }
                  public interface MyTest4 {
                  }
                }
                """
        )
    }

    @Test
    fun `Interfaces should not be dropped`() {
        check(
            checkCompatibility = true,
            warnings = """
                TESTROOT/load-api.txt:2: error: Class test.pkg.MyTest1 changed class/interface declaration [ChangedClass:23]
                TESTROOT/load-api.txt:4: error: Class test.pkg.MyTest2 changed class/interface declaration [ChangedClass:23]
                """,
            compatibilityMode = false,
            previousApi = """
                package test.pkg {
                  public class MyTest1 {
                  }
                  public interface MyTest2 {
                  }
                  public class MyTest3 {
                  }
                  public interface MyTest4 {
                  }
                }
                """,
            // MyTest1 and MyTest2 reversed from class to interface or vice versa, MyTest3 and MyTest4 unchanged
            signatureSource = """
                package test.pkg {
                  public interface MyTest1 {
                  }
                  public class MyTest2 {
                  }
                  public class MyTest3 {
                  }
                  public interface MyTest4 {
                  }
                }
                """
        )
    }

    @Test
    fun `Ensure warnings for removed APIs`() {
        check(
            checkCompatibility = true,
            warnings = """
                TESTROOT/previous-api.txt:3: error: Removed method test.pkg.MyTest1.method [RemovedMethod:9]
                TESTROOT/previous-api.txt:4: error: Removed field test.pkg.MyTest1.field [RemovedField:10]
                TESTROOT/previous-api.txt:6: error: Removed class test.pkg.MyTest2 [RemovedClass:8]
                """,
            compatibilityMode = false,
            previousApi = """
                package test.pkg {
                  public class MyTest1 {
                    method public Double method(Float);
                    field public Double field;
                  }
                  public class MyTest2 {
                    method public Double method(Float);
                    field public Double field;
                  }
                }
                package test.pkg.other {
                }
                """,
            signatureSource = """
                package test.pkg {
                  public class MyTest1 {
                  }
                }
                """,
            api = """
                package test.pkg {
                  public class MyTest1 {
                  }
                }
                """
        )
    }

    @Test
    fun `Flag invalid nullness changes`() {
        check(
            checkCompatibility = true,
            warnings = """
                TESTROOT/load-api.txt:5: error: Attempted to remove @Nullable annotation from method test.pkg.MyTest.convert3 [InvalidNullConversion:40]
                TESTROOT/load-api.txt:5: error: Attempted to remove @Nullable annotation from parameter arg1 in test.pkg.MyTest.convert3 [InvalidNullConversion:40]
                TESTROOT/load-api.txt:6: error: Attempted to remove @NonNull annotation from method test.pkg.MyTest.convert4 [InvalidNullConversion:40]
                TESTROOT/load-api.txt:6: error: Attempted to remove @NonNull annotation from parameter arg1 in test.pkg.MyTest.convert4 [InvalidNullConversion:40]
                TESTROOT/load-api.txt:7: error: Attempted to change parameter from @Nullable to @NonNull: incompatible change for parameter arg1 in test.pkg.MyTest.convert5 [InvalidNullConversion:40]
                TESTROOT/load-api.txt:8: error: Attempted to change method return from @NonNull to @Nullable: incompatible change for method test.pkg.MyTest.convert6 [InvalidNullConversion:40]
                """,
            compatibilityMode = false,
            outputKotlinStyleNulls = false,
            previousApi = """
                package test.pkg {
                  public class MyTest {
                    method public Double convert1(Float);
                    method public Double convert2(Float);
                    method @Nullable public Double convert3(@Nullable Float);
                    method @NonNull public Double convert4(@NonNull Float);
                    method @Nullable public Double convert5(@Nullable Float);
                    method @NonNull public Double convert6(@NonNull Float);
                  }
                }
                """,
            // Changes: +nullness, -nullness, nullable->nonnull, nonnull->nullable
            signatureSource = """
                package test.pkg {
                  public class MyTest {
                    method @Nullable public Double convert1(@Nullable Float);
                    method @NonNull public Double convert2(@NonNull Float);
                    method public Double convert3(Float);
                    method public Double convert4(Float);
                    method @NonNull public Double convert5(@NonNull Float);
                    method @Nullable public Double convert6(@Nullable Float);
                  }
                }
                """,
            api = """
                package test.pkg {
                  public class MyTest {
                    method @Nullable public Double convert1(@Nullable Float);
                    method @NonNull public Double convert2(@NonNull Float);
                    method public Double convert3(Float);
                    method public Double convert4(Float);
                    method @NonNull public Double convert5(@NonNull Float);
                    method @Nullable public Double convert6(@Nullable Float);
                  }
                }
                """
        )
    }

    @Test
    fun `Kotlin Nullness`() {
        check(
            checkCompatibility = true,
            warnings = """
                src/test/pkg/Outer.kt:5: error: Attempted to change method return from @NonNull to @Nullable: incompatible change for method test.pkg.Outer.method2 [InvalidNullConversion:40]
                src/test/pkg/Outer.kt:5: error: Attempted to change parameter from @Nullable to @NonNull: incompatible change for parameter string in test.pkg.Outer.method2 [InvalidNullConversion:40]
                src/test/pkg/Outer.kt:6: error: Attempted to change parameter from @Nullable to @NonNull: incompatible change for parameter string in test.pkg.Outer.method3 [InvalidNullConversion:40]
                src/test/pkg/Outer.kt:8: error: Attempted to change method return from @NonNull to @Nullable: incompatible change for method test.pkg.Outer.Inner.method2 [InvalidNullConversion:40]
                src/test/pkg/Outer.kt:8: error: Attempted to change parameter from @Nullable to @NonNull: incompatible change for parameter string in test.pkg.Outer.Inner.method2 [InvalidNullConversion:40]
                src/test/pkg/Outer.kt:9: error: Attempted to change parameter from @Nullable to @NonNull: incompatible change for parameter string in test.pkg.Outer.Inner.method3 [InvalidNullConversion:40]
                """,
            compatibilityMode = false,
            inputKotlinStyleNulls = true,
            outputKotlinStyleNulls = true,
            previousApi = """
                    package test.pkg {
                      public final class Outer {
                        ctor public Outer();
                        method public final String? method1(String, String?);
                        method public final String method2(String?, String);
                        method public final String? method3(String, String?);
                      }
                      public static final class Outer.Inner {
                        ctor public Outer.Inner();
                        method public final String method2(String?, String);
                        method public final String? method3(String, String?);
                      }
                    }
                """,
            sourceFiles = *arrayOf(
                kotlin(
                    """
                    package test.pkg

                    class Outer {
                        fun method1(string: String, maybeString: String?): String? = null
                        fun method2(string: String, maybeString: String?): String? = null
                        fun method3(maybeString: String?, string : String): String = ""
                        class Inner {
                            fun method2(string: String, maybeString: String?): String? = null
                            fun method3(maybeString: String?, string : String): String = ""
                        }
                    }
                    """
                )
            ),
            api = """
                package test.pkg {
                  public final class Outer {
                    ctor public Outer();
                    method public final String? method1(String string, String? maybeString);
                    method public final String? method2(String string, String? maybeString);
                    method public final String method3(String? maybeString, String string);
                  }
                  public static final class Outer.Inner {
                    ctor public Outer.Inner();
                    method public final String? method2(String string, String? maybeString);
                    method public final String method3(String? maybeString, String string);
                  }
                }
                """
        )
    }

    @Test
    fun `Java Parameter Name Change`() {
        check(
            checkCompatibility = true,
            warnings = """
                src/test/pkg/JavaClass.java:6: error: Attempted to remove parameter name from parameter newName in test.pkg.JavaClass.method1 in method test.pkg.JavaClass.method1 [ParameterNameChange:41]
                src/test/pkg/JavaClass.java:7: error: Attempted to change parameter name from secondParameter to newName in method test.pkg.JavaClass.method2 [ParameterNameChange:41]
                """,
            compatibilityMode = false,
            previousApi = """
                package test.pkg {
                  public class JavaClass {
                    ctor public JavaClass();
                    method public String method1(String parameterName);
                    method public String method2(String firstParameter, String secondParameter);
                  }
                }
                """,
            sourceFiles = *arrayOf(
                java(
                    """
                    @Suppress("all")
                    package test.pkg;
                    import android.support.annotation.ParameterName;

                    public class JavaClass {
                        public String method1(String newName) { return null; }
                        public String method2(@ParameterName("firstParameter") String s, @ParameterName("newName") String prevName) { return null; }
                    }
                    """
                ),
                supportParameterName
            ),
            api = """
                package test.pkg {
                  public class JavaClass {
                    ctor public JavaClass();
                    method public String! method1(String!);
                    method public String! method2(String! firstParameter, String! newName);
                  }
                }
                """,
            extraArguments = arrayOf("--hide-package", "android.support.annotation")
        )
    }

    @Test
    fun `Kotlin Parameter Name Change`() {
        check(
            checkCompatibility = true,
            warnings = """
                src/test/pkg/KotlinClass.kt:4: error: Attempted to change parameter name from prevName to newName in method test.pkg.KotlinClass.method1 [ParameterNameChange:41]
                """,
            compatibilityMode = false,
            inputKotlinStyleNulls = true,
            outputKotlinStyleNulls = true,
            previousApi = """
                package test.pkg {
                  public final class KotlinClass {
                    ctor public KotlinClass();
                    method public final String? method1(String prevName);
                  }
                }
                """,
            sourceFiles = *arrayOf(
                kotlin(
                    """
                    package test.pkg

                    class KotlinClass {
                        fun method1(newName: String): String? = null
                    }
                    """
                )
            ),
            api = """
                package test.pkg {
                  public final class KotlinClass {
                    ctor public KotlinClass();
                    method public final String? method1(String newName);
                  }
                }
                """
        )
    }

    @Test
    fun `Add flag new methods but not overrides from platform`() {
        check(
            checkCompatibility = true,
            warnings = """
                src/test/pkg/MyClass.java:6: error: Added method test.pkg.MyClass.method2 [AddedMethod:4]
                src/test/pkg/MyClass.java:7: error: Added field test.pkg.MyClass.newField [AddedField:5]
                """,
            compatibilityMode = false,
            previousApi = """
                package test.pkg {
                  public class MyClass {
                    method public String method1(String);
                  }
                }
                """,
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;

                    public class MyClass  {
                        private MyClass() { }
                        public String method1(String newName) { return null; }
                        public String method2(String newName) { return null; }
                        public int newField = 5;
                        public String toString() { return "Hello World"; }
                    }
                    """
                )
            )
        )
    }

    @Test
    fun `Remove operator`() {
        check(
            checkCompatibility = true,
            warnings = """
                src/test/pkg/Foo.kt:4: error: Cannot remove `operator` modifier from method test.pkg.Foo.plus: Incompatible change [OperatorRemoval:42]
                """,
            compatibilityMode = false,
            previousApi = """
                package test.pkg {
                  public final class Foo {
                    ctor public Foo();
                    method public final operator void plus(String s);
                  }
                }
                """,
            sourceFiles = *arrayOf(
                kotlin(
                    """
                    package test.pkg

                    class Foo {
                        fun plus(s: String) { }
                    }
                    """
                )
            )
        )
    }

    @Test
    fun `Remove vararg`() {
        check(
            checkCompatibility = true,
            warnings = """
                src/test/pkg/test.kt:3: error: Changing from varargs to array is an incompatible change: parameter x in test.pkg.TestKt.method2 [VarargRemoval:44]
                """,
            compatibilityMode = false,
            previousApi = """
                package test.pkg {
                  public final class TestKt {
                    ctor public TestKt();
                    method public static final void method1(int[] x);
                    method public static final void method2(int... x);
                  }
                }
                """,
            api = """
                package test.pkg {
                  public final class TestKt {
                    ctor public TestKt();
                    method public static final void method1(int... x);
                    method public static final void method2(int[] x);
                  }
                }
                """,
            sourceFiles = *arrayOf(
                kotlin(
                    """
                    package test.pkg
                    fun method1(vararg x: Int) { }
                    fun method2(x: IntArray) { }
                    """
                )
            )
        )
    }

    @Test
    fun `Add final`() {
        // Adding final on class or method is incompatible; adding it on a parameter is fine.
        // Field is iffy.
        check(
            checkCompatibility = true,
            warnings = """
                src/test/pkg/Java.java:4: error: Making a class or method final is an incompatible change: method test.pkg.Java.method [NewlyFinal:45]
                src/test/pkg/Kotlin.kt:4: error: Making a class or method final is an incompatible change: method test.pkg.Kotlin.method [NewlyFinal:45]
                """,
            compatibilityMode = false,
            previousApi = """
                package test.pkg {
                  public class Java {
                    method public void method(int);
                  }
                  public class Kotlin {
                    ctor public Kotlin();
                    method public void method(String s);
                  }
                }
                """,
            sourceFiles = *arrayOf(
                kotlin(
                    """
                    package test.pkg

                    open class Kotlin {
                        fun method(s: String) { }
                    }
                    """
                ),
                java(
                    """
                        package test.pkg;
                        public class Java {
                            private Java() { }
                            public final void method(final int parameter) { }
                        }
                        """
                )
            )
        )
    }

    @Test
    fun `Remove infix`() {
        check(
            checkCompatibility = true,
            warnings = """
                src/test/pkg/Foo.kt:5: error: Cannot remove `infix` modifier from method test.pkg.Foo.add2: Incompatible change [InfixRemoval:43]
                """,
            compatibilityMode = false,
            previousApi = """
                package test.pkg {
                  public final class Foo {
                    ctor public Foo();
                    method public final void add1(String s);
                    method public final infix void add2(String s);
                    method public final infix void add3(String s);
                  }
                }
                """,
            sourceFiles = *arrayOf(
                kotlin(
                    """
                    package test.pkg

                    class Foo {
                        infix fun add1(s: String) { }
                        fun add2(s: String) { }
                        infix fun add3(s: String) { }
                    }
                    """
                )
            )
        )
    }

    @Test
    fun `Add seal`() {
        check(
            checkCompatibility = true,
            warnings = """
                src/test/pkg/Foo.kt: error: Cannot add `sealed` modifier to class test.pkg.Foo: Incompatible change [AddSealed:46]
                """,
            compatibilityMode = false,
            previousApi = """
                package test.pkg {
                  public class Foo {
                  }
                }
                """,
            sourceFiles = *arrayOf(
                kotlin(
                    """
                    package test.pkg
                    sealed class Foo {
                    }
                    """
                )
            )
        )
    }

    @Test
    fun `Remove default parameter`() {
        check(
            checkCompatibility = true,
            warnings = """
                src/test/pkg/Foo.kt:7: error: Attempted to remove default value from parameter s1 in test.pkg.Foo.method4 in method test.pkg.Foo.method4 [DefaultValueChange:50]
                """,
            compatibilityMode = false,
            inputKotlinStyleNulls = true,
            previousApi = """
                package test.pkg {
                  public final class Foo {
                    ctor public Foo();
                    method public final void method1(boolean b, String? s1);
                    method public final void method2(boolean b, String? s1);
                    method public final void method3(boolean b, String? s1 = "null");
                    method public final void method4(boolean b, String? s1 = "null");
                  }
                }
                """,
            sourceFiles = *arrayOf(
                kotlin(
                    """
                    package test.pkg

                    class Foo {
                        fun method1(b: Boolean, s1: String?) { }         // No change
                        fun method2(b: Boolean, s1: String? = null) { }  // Adding: OK
                        fun method3(b: Boolean, s1: String? = null) { }  // No change
                        fun method4(b: Boolean, s1: String?) { }         // Removed
                    }
                    """
                )
            )
        )
    }

    // TODO: Check method signatures changing incompatibly (look especially out for adding new overloaded
    // methods and comparator getting confused!)
    //   ..equals on the method items should actually be very useful!
}