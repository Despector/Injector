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
package com.voxelgenesis.injector.launch;

import com.voxelgenesis.injector.InjectionManager;
import com.voxelgenesis.injector.target.TypeInjector;
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
        TypeInjector injection = InjectionManager.get().getInjection(transformedName);
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
