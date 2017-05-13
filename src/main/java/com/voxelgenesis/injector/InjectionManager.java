/*
 * The MIT License (MIT)
 *
 * Copyright (c) Despector <https://despector.voxelgenesis.com>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.voxelgenesis.injector;

import com.voxelgenesis.injector.target.TypeInjector;
import org.spongepowered.despector.ast.Annotation;
import org.spongepowered.despector.ast.AnnotationType;
import org.spongepowered.despector.ast.SourceSet;
import org.spongepowered.despector.ast.generic.ClassTypeSignature;
import org.spongepowered.despector.ast.type.TypeEntry;
import org.spongepowered.despector.decompiler.Decompilers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class InjectionManager {

    private static final InjectionManager instance = new InjectionManager();

    public static InjectionManager get() {
        return instance;
    }

    private final SourceSet injector_sourceset = new SourceSet();
    private final Map<String, TypeInjector> targets = new HashMap<>();

    private final AnnotationType injection_annotation;

    private InjectionManager() {
        this.injection_annotation = this.injector_sourceset.getAnnotationType("com/voxelgenesis/injector/Injector");
    }

    public SourceSet getSourceSet() {
        return this.injector_sourceset;
    }

    public void addInjector(Class<?> src, String injector) {
        String path = src.getProtectionDomain().getCodeSource().getLocation().getPath();
        File file = new File(path, injector.replace('.', '/') + ".class");
        try {
            TypeEntry type = Decompilers.WILD.decompile(file, this.injector_sourceset);
            Decompilers.WILD.flushTasks();
            Annotation anno = type.getAnnotation(this.injection_annotation);
            if (anno == null) {
                System.err.println("Injector " + injector + " is missing the @Injector annotation");
                return;
            }
            String target = anno.<ClassTypeSignature>getValue("value").getClassName();
            this.targets.put(target, new TypeInjector(target, type));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public TypeInjector getInjection(String target) {
        return this.targets.get(target);
    }

}
