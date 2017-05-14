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
package com.voxelgenesis.injector.target;

import com.voxelgenesis.injector.target.match.InjectionMatcher;
import com.voxelgenesis.injector.target.parse.MatchParser;
import org.spongepowered.despector.ast.Annotation;
import org.spongepowered.despector.ast.AnnotationType;
import org.spongepowered.despector.ast.generic.ClassTypeSignature;
import org.spongepowered.despector.ast.type.MethodEntry;
import org.spongepowered.despector.ast.type.TypeEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypeInjector {

    private final String target;
    private final TypeEntry injector;

    private final Map<String, InjectionTarget> targets = new HashMap<>();

    public TypeInjector(String target, TypeEntry type) {
        this.target = target;
        this.injector = type;
    }

    public String getTarget() {
        return this.target;
    }

    public TypeEntry getInjector() {
        return this.injector;
    }

    private void buildTargets() {
        AnnotationType inject_anno = this.injector.getSource().getAnnotationType("com/voxelgenesis/injector/Inject");
        for (MethodEntry mth : this.injector.getMethods()) {
            Annotation inject = mth.getAnnotation(inject_anno);
            if (inject != null) {
                String target = inject.getValue("target");
                String matcher = inject.getValue("matcher");
                InjectionTarget itarget = this.targets.get(target);
                if (itarget == null) {
                    itarget = new InjectionTarget(target);
                    this.targets.put(target, itarget);
                }
                MatchParser parser = new MatchParser(matcher, mth);
                List<ClassTypeSignature> imports = inject.getValue("imports");
                for (ClassTypeSignature im : imports) {
                    String type = im.getDescriptor();
                    type = type.substring(1, type.length() - 1);
                    String simple = type.substring(type.lastIndexOf('/') + 1);
                    parser.addImport(simple, im.getDescriptor());
                }
                InjectionMatcher imatcher = parser.parse();
                itarget.addInjection(imatcher);
            }
        }

    }

    public void apply(TypeEntry type) {
        buildTargets();

        for (MethodEntry mth : type.getMethods()) {
            InjectionTarget target = this.targets.get(mth.getName() + mth.getDescription());
            if (target != null) {
                target.apply(mth);
            }
        }
    }

}
