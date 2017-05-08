/*
 * Copyright (c) 2015-2016 VoxelBox <http://engine.thevoxelbox.com>.
 * All Rights Reserved.
 */
package com.voxelgenesis.injector;

import com.voxelgenesis.injector.target.InjectionTarget;
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
    private final Map<String, InjectionTarget> targets = new HashMap<>();

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
            Annotation anno = type.getAnnotation(this.injection_annotation);
            if (anno == null) {
                System.err.println("Injector " + injector + " is missing the @Injector annotation");
                return;
            }
            String target = anno.<ClassTypeSignature>getValue("value").getClassName();
            this.targets.put(target, new InjectionTarget(target, type));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public InjectionTarget getInjection(String target) {
        return this.targets.get(target);
    }

}
