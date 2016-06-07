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
 *
 */

package com.android.okhttp.srcgen;

import com.google.currysrc.Main;
import com.google.currysrc.api.Rules;
import com.google.currysrc.api.input.DirectoryInputFileGenerator;
import com.google.currysrc.api.input.InputFileGenerator;
import com.google.currysrc.api.match.SourceMatchers;
import com.google.currysrc.api.output.BasicOutputSourceFileGenerator;
import com.google.currysrc.api.output.OutputSourceFileGenerator;
import com.google.currysrc.api.process.DefaultRule;
import com.google.currysrc.api.process.Processor;
import com.google.currysrc.api.process.Rule;
import com.google.currysrc.processors.ModifyQualifiedNames;
import com.google.currysrc.processors.ModifyStringLiterals;
import com.google.currysrc.processors.RenamePackage;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Created by tobiast on 6/15/16.
 */
public class OkioTransform {
    static final String ORIGINAL_PACKAGE = "okio";
    static final String ANDROID_PACKAGE = "com.android.okhttp.okio";

    public static void main(String[] args) throws Exception {
        new Main(false /* debug */).execute(new LocalRules());
    }

    static class LocalRules implements Rules {
        @Override
        public InputFileGenerator getInputFileGenerator() {
            return new DirectoryInputFileGenerator(new File("external/okhttp/okio/okio/src/main/java"));
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
            File outputDir = new File("external/okhttp/android-generated");
            return new BasicOutputSourceFileGenerator(outputDir);
        }
    }

    public static DefaultRule createMandatoryRule(Processor processor) {
        return new DefaultRule(processor, SourceMatchers.all(), true /* mustModify */);
    }

    public static DefaultRule createOptionalRule(Processor processor) {
        return new DefaultRule(processor, SourceMatchers.all(), false /* mustModify */);
    }

    private OkioTransform() {
    }
}
