/*
 * Copyright (C) 2019 Square, Inc.
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
#ifndef DUKTAPE_ANDROID_JSMETHODPROXY_H
#define DUKTAPE_ANDROID_JSMETHODPROXY_H

#include <jni.h>
#include <string>
#include <functional>
#include <vector>
#include "quickjs/quickjs.h"

class Context;

class JsMethodProxy {
public:
  JsMethodProxy(const Context* context, const char* name, jobject method);

  jobject call(Context *context, JSValue thisPointer, jobjectArray pArray) const;

  const std::string name;
  const jmethodID methodId;
private:
  std::vector<std::function<JSValueConst(const Context*, jvalue)>> argumentLoaders;
  std::function<jvalue(const Context*, const JSValueConst&)> resultLoader;
  bool isVarArgs;
};


#endif //DUKTAPE_ANDROID_JSMETHODPROXY_H
