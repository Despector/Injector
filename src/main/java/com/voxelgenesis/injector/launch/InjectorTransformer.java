/*
 * Copyright (c) 2015-2016 VoxelBox <http://engine.thevoxelbox.com>.
 * All Rights Reserved.
 */
package com.voxelgenesis.injector.launch;

import com.voxelgenesis.injector.InjectionManager;
import com.voxelgenesis.injector.target.InjectionTarget;
import net.minecraft.launchwrapper.IClassTransformer;
import org.spongepowered.despector.ast.type.TypeEntry;
import org.spongepowered.despector.decompiler.Decompilers;
import org.spongepowered.despector.emitter.Emitters;
import org.spongepowered.despector.emitter.bytecode.BytecodeEmitterContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class InjectorTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        InjectionTarget injection = InjectionManager.get().getInjection(transformedName);
        if (injection != null) {
            try {
                TypeEntry type = Decompilers.JAVA.decompile(new ByteArrayInputStream(basicClass), InjectionManager.get().getSourceSet());
                injection.apply(type);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                BytecodeEmitterContext ctx = new BytecodeEmitterContext(out);
                Emitters.BYTECODE.emit(ctx, type);
                return out.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
                return basicClass;
            }
        }
        return basicClass;
    }

}
