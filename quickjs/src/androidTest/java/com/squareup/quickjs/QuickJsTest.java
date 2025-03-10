/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.quickjs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

public final class QuickJsTest {
  private QuickJs quickjs;

  @Before public void setUp() {
    quickjs = QuickJs.create();
  }

  @After public void tearDown() {
    quickjs.close();
  }

  @Test public void helloWorld() {
    String hello = (String) quickjs.evaluate("'hello, world!'.toUpperCase();");
    assertThat(hello).isEqualTo("HELLO, WORLD!");
  }

  @Test public void exceptionsInScriptThrowInJava() {
    try {
      quickjs.evaluate("nope();");
      fail();
    } catch (QuickJsException e) {
      assertThat(e).hasMessageThat().isEqualTo("nope is not defined");
    }
  }

  @Test public void returnTypes() {
    assertThat(quickjs.evaluate("\"test\";")).isEqualTo("test");

    assertThat(quickjs.evaluate("true;")).isEqualTo(true);
    assertThat(quickjs.evaluate("false;")).isEqualTo(false);

    assertThat(quickjs.evaluate("1;")).isEqualTo(1);
    assertThat(quickjs.evaluate("1.123;")).isEqualTo(1.123);

    assertThat(quickjs.evaluate("undefined;")).isNull();
    assertThat(quickjs.evaluate("null;")).isNull();
  }

  @Test public void exceptionsInScriptIncludeStackTrace() {
    try {
      quickjs.evaluate("\n"
          + "f1();\n"           // Line 2.
          + "\n"
          + "function f1() {\n"
          + "  f2();\n"         // Line 5.
          + "}\n"
          + "\n"
          + "\n"
          + "function f2() {\n"
          + "  nope();\n"       // Line 10.
          + "}\n", "test.js");
      fail();
    } catch (QuickJsException e) {
      assertThat(e).hasMessageThat().isEqualTo("nope is not defined");
      assertThat(e.getStackTrace()).asList().containsAtLeast(
          new StackTraceElement("JavaScript", "<eval>", "test.js", 2),
          new StackTraceElement("JavaScript", "f1", "test.js", 5),
          new StackTraceElement("JavaScript", "f2", "test.js", 10));
    }
  }
}
