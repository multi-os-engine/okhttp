/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.okhttp.srcgen;

import com.google.currysrc.Main;
import com.google.currysrc.api.Rules;
import com.google.currysrc.api.input.CompoundDirectoryInputFileGenerator;
import com.google.currysrc.api.input.DirectoryInputFileGenerator;
import com.google.currysrc.api.input.InputFileGenerator;
import com.google.currysrc.api.match.SourceMatchers;
import com.google.currysrc.api.output.BasicOutputSourceFileGenerator;
import com.google.currysrc.api.output.OutputSourceFileGenerator;
import com.google.currysrc.api.process.DefaultRule;
import com.google.currysrc.api.process.Processor;
import com.google.currysrc.api.process.Rule;
import com.google.currysrc.processors.InsertHeader;
import com.google.currysrc.processors.ModifyQualifiedNames;
import com.google.currysrc.processors.ModifyStringLiterals;
import com.google.currysrc.processors.RenamePackage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Generates OkHttp sources in the packages into which jarjar also transforms them.
 */
public class OkHttpTransform {
    static final String ORIGINAL_PACKAGE = "com.squareup.okhttp";
    static final String ANDROID_PACKAGE = "com.android.okhttp";

    /**
     * Usage:
     * java OkHttpTransform {source files/directories} {target dir}
     */
    public static void main(String[] args) throws Exception {
        new Main(false /* debug */).execute(new OkHttpRules());
    }

    static class OkHttpRules implements Rules {
        private final String basePath = "external/okhttp/";

        @Override
        public InputFileGenerator getInputFileGenerator() {
            List<String> paths = new ArrayList<>();
            // TODO: Move those to a src/ subdirector to be consistent
            paths.add(basePath + "android/main/java");
            paths.add(basePath + "android/test/java");

            List<String> dirNames = Arrays.asList(
                    "benchmarks",
                    "mockwebserver",
                    "okcurl",
                    "okhttp",
                    "okhttp-android-support",
                    "okhttp-apache",
                    "okhttp-hpacktests",
                    "okhttp-logging-interceptor",
                    "okhttp-testing-support",
                    "okhttp-tests",
                    "okhttp-urlconnection",
                    "okhttp-ws",
                    "okhttp-ws-tests",
                    "okio"
                    );
            for (String dirName : dirNames) {
                paths.add(basePath + dirName + "/src/main/java");
                paths.add(basePath + dirName + "/src/test/java");
            }
            List<InputFileGenerator> inputFileGenerators = new ArrayList<>();
            for (String path : paths) {
                File file = new File(path);
                if (file.exists()) {
                    inputFileGenerators.add(new DirectoryInputFileGenerator(file));
                }
            }
            if (inputFileGenerators.isEmpty()) {
                throw new AssertionError("Files not found: " + paths);
            }
            return new CompoundDirectoryInputFileGenerator(inputFileGenerators);
        }

        @Override
        public List<Rule> getRuleList(File ignored) {
            return Arrays.asList(
                    // Doc change: Insert a warning about the source code being generated.
                    // This changes all the line numbers below, so only use if you're not debugging
//                    createMandatoryRule(new InsertHeader("/* GENERATED SOURCE. DO NOT MODIFY. */\n")),
                    // AST change: Change the package of each CompilationUnit
                    createMandatoryRule(new RenamePackage(ORIGINAL_PACKAGE, ANDROID_PACKAGE)),
                    // AST change: Change all qualified names in code and javadoc.
                    createOptionalRule(new ModifyQualifiedNames(ORIGINAL_PACKAGE, ANDROID_PACKAGE)),
                    // AST change: Change all string literals containing package names in code.
                    createOptionalRule(new ModifyStringLiterals(ORIGINAL_PACKAGE, ANDROID_PACKAGE))
                    );
        }

        @Override
        public OutputSourceFileGenerator getOutputSourceFileGenerator() {
            File outputDir = new File(basePath, "android-generated");
            return new BasicOutputSourceFileGenerator(outputDir);
        }
    }

    public static DefaultRule createMandatoryRule(Processor processor) {
        return new DefaultRule(processor, SourceMatchers.all(), true /* mustModify */);
    }

    public static DefaultRule createOptionalRule(Processor processor) {
        return new DefaultRule(processor, SourceMatchers.all(), false /* mustModify */);
    }

    private OkHttpTransform() {
    }
}
