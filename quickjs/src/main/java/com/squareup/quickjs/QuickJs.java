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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.Closeable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

/** An EMCAScript (Javascript) interpreter backed by the 'QuickJS' native engine. */
public final class QuickJs implements Closeable {
  static {
    System.loadLibrary("quickjs");
  }

  /**
   * Create a new interpreter instance. Calls to this method <strong>must</strong> matched with
   * calls to {@link #close()} on the returned instance to avoid leaking native memory.
   */
  @NonNull
  public static QuickJs create() {
    long context = createContext();
    if (context == 0) {
      throw new OutOfMemoryError("Cannot create QuickJs instance");
    }
    return new QuickJs(context);
  }

  private long context;

  private QuickJs(long context) {
    this.context = context;
  }

  /**
   * Evaluate {@code script} and return any result. {@code fileName} will be used in error
   * reporting.
   *
   * @throws QuickJsException if there is an error evaluating the script.
   */
  @Nullable
  public synchronized Object evaluate(@NonNull String script, @NonNull String fileName) {
    return evaluate(context, script, fileName);
  }

  /**
   * Evaluate {@code script} and return a result.
   *
   * @throws QuickJsException if there is an error evaluating the script.
   */
  @Nullable
  public synchronized Object evaluate(@NonNull String script) {
    return evaluate(context, script, "?");
  }

  /**
   * Attaches to a global JavaScript object called {@code name} that implements {@code type}.
   * {@code type} defines the interface implemented in JavaScript that will be accessible to Java.
   * {@code type} must be an interface that does not extend any other interfaces, and cannot define
   * any overloaded methods.
   * <p>Methods of the interface may return {@code void} or any of the following supported argument
   * types: {@code boolean}, {@link Boolean}, {@code int}, {@link Integer}, {@code double},
   * {@link Double}, {@link String}.
   */
  @NonNull
  public synchronized <T> T get(@NonNull final String name, @NonNull final Class<T> type) {
    if (!type.isInterface()) {
      throw new UnsupportedOperationException("Only interfaces can be proxied. Received: " + type);
    }
    if (type.getInterfaces().length > 0) {
      throw new UnsupportedOperationException(type + " must not extend other interfaces");
    }
    LinkedHashMap<String, Method> methods = new LinkedHashMap<>();
    for (Method method : type.getMethods()) {
      if (methods.put(method.getName(), method) != null) {
        throw new UnsupportedOperationException(method.getName() + " is overloaded in " + type);
      }
    }

    final long instance = get(context, name, methods.values().toArray());
    if (instance == 0) {
      throw new OutOfMemoryError("Cannot create QuickJs proxy to " + name);
    }
    final QuickJs quickJs = this;

    Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
        new InvocationHandler() {
          @Override
          public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // If the method is a method from Object then defer to normal invocation.
            if (method.getDeclaringClass() == Object.class) {
              return method.invoke(this, args);
            }
            synchronized (quickJs) {
              return call(quickJs.context, instance, method, args);
            }
          }

          @Override
          public String toString() {
            return String.format("QuickJsProxy{name=%s, type=%s}", name, type.getName());
          }
        });
    return (T) proxy;
  }

  /**
   * Release the native resources associated with this object. You <strong>must</strong> call this
   * method for each instance to avoid leaking native memory.
   */
  @Override public synchronized void close() {
    if (context != 0) {
      long contextToClose = context;
      context = 0;
      destroyContext(contextToClose);
    }
  }

  @Override protected synchronized void finalize() {
    if (context != 0) {
      Logger.getLogger(getClass().getName()).warning("QuickJs instance leaked!");
    }
  }

  private static native long createContext();

  private native void destroyContext(long context);

  private native Object evaluate(long context, String sourceCode, String fileName);

  private native long get(long context, String name, Object[] methods);

  private native Object call(long context, long instance, Object method, Object[] args);
}
