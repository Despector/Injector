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
package com.voxelgenesis.injector.target.match.modifier;

import com.voxelgenesis.injector.target.match.InjectionModifier;
import org.spongepowered.despector.ast.Annotation;
import org.spongepowered.despector.ast.Locals.Local;
import org.spongepowered.despector.ast.Locals.LocalInstance;
import org.spongepowered.despector.ast.insn.Instruction;
import org.spongepowered.despector.ast.insn.condition.Condition;
import org.spongepowered.despector.ast.insn.cst.StringConstant;
import org.spongepowered.despector.ast.stmt.Statement;
import org.spongepowered.despector.ast.stmt.assign.LocalAssignment;
import org.spongepowered.despector.ast.type.MethodEntry;
import org.spongepowered.despector.transform.matcher.MatchContext;
import org.spongepowered.despector.util.TypeHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatementInsertModifier implements InjectionModifier {

    private final MethodEntry injector;

    public StatementInsertModifier(MethodEntry injector) {
        this.injector = injector;
    }

    @Override
    public void apply(List<Statement> statements, int start, int end, MethodEntry target, MatchContext match) {
        Map<LocalInstance, LocalInstance> local_translation = buildLocalTranslation(target, match, start);

        for (int i = this.injector.getInstructions().size() - 2; i >= 0; i--) {
            Statement stmt = this.injector.getInstructions().get(i);
            statements.add(start, translate(stmt, local_translation));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Statement> T translate(T stmt, Map<LocalInstance, LocalInstance> local_translation) {
        Translator<T> trans = (Translator<T>) statement_translators.get(stmt.getClass());
        if (trans == null) {
            throw new IllegalStateException("No translator for statement type: " + stmt.getClass().getName());
        }
        return trans.translate(stmt, local_translation);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Instruction> T translate(T stmt, Map<LocalInstance, LocalInstance> local_translation) {
        Translator<T> trans = (Translator<T>) instruction_translators.get(stmt.getClass());
        if (trans == null) {
            throw new IllegalStateException("No translator for instruction type: " + stmt.getClass().getName());
        }
        return trans.translate(stmt, local_translation);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Condition> T translate(T stmt, Map<LocalInstance, LocalInstance> local_translation) {
        Translator<T> trans = (Translator<T>) condition_translators.get(stmt.getClass());
        if (trans == null) {
            throw new IllegalStateException("No translator for condition type: " + stmt.getClass().getName());
        }
        return trans.translate(stmt, local_translation);
    }

    private Map<LocalInstance, LocalInstance> buildLocalTranslation(MethodEntry target, MatchContext match, int offs) {
        Map<LocalInstance, LocalInstance> local_translation = new HashMap<>();
        int local_index = 0;
        if (!this.injector.isStatic()) {
            LocalInstance inj_this = this.injector.getLocals().getLocal(0).getParameterInstance();
            LocalInstance target_this = target.getLocals().getLocal(0).getParameterInstance();
            local_translation.put(inj_this, target_this);
            local_index++;
        }
        for (String p : TypeHelper.splitSig(this.injector.getDescription())) {
            LocalInstance inj_param = this.injector.getLocals().getLocal(local_index).getParameterInstance();
            boolean handled = false;
            for (Annotation anno : inj_param.getAnnotations()) {
                if (anno.getType().getName().equals("com/voxelgenesis/injector/Local")) {
                    LocalInstance target_local = match.getLocal(anno.getValue("value"));
                    local_translation.put(inj_param, target_local);
                    handled = true;
                    break;
                }
            }

            if (!handled) {
                throw new IllegalStateException();
            }

            local_index++;
            if (p.equals("D") || p.equals("J")) {
                local_index++;
            }
        }

        int highest_local = 0;
        if (!target.isStatic()) {
            highest_local++;
        }
        for (String p : TypeHelper.splitSig(target.getDescription())) {
            highest_local++;
            if (p.equals("D") || p.equals("J")) {
                highest_local++;
            }
        }
        for (LocalInstance instance : target.getLocals().getAllInstances()) {
            int local = instance.getLocal().getIndex();
            if (local > highest_local) {
                highest_local = local;
            }
        }
        highest_local++;
        Map<Integer, Integer> local_index_mapping = new HashMap<>();
        for (LocalInstance instance : this.injector.getLocals().getAllInstances()) {
            if (local_translation.containsKey(instance)) {
                continue;
            }
            if (instance.getLocal().getIndex() < local_index) {
                throw new IllegalStateException();
            }
            Integer new_index = local_index_mapping.get(instance.getLocal().getIndex());
            if (new_index == null) {
                new_index = highest_local++;
                if (instance.getType().getDescriptor().equals("D") || instance.getType().getDescriptor().equals("J")) {
                    highest_local++;
                }
                local_index_mapping.put(instance.getLocal().getIndex(), new_index);
            }
            Local new_local = target.getLocals().getLocal(new_index);
            int start = instance.getStart();
            int end = instance.getEnd() + offs;
            if (start >= 0) {
                start += offs;
            }
            LocalInstance new_instance = new LocalInstance(new_local, instance.getName(), instance.getType(), start, end);
            new_local.addInstance(new_instance);
            local_translation.put(instance, new_instance);
        }

        return local_translation;
    }

    private static final Map<Class<?>, Translator<?>> statement_translators = new HashMap<>();
    private static final Map<Class<?>, Translator<?>> instruction_translators = new HashMap<>();
    private static final Map<Class<?>, Translator<?>> condition_translators = new HashMap<>();

    private static <T extends Statement> void registerStatementTranslator(Class<T> type, Translator<T> trans) {
        statement_translators.put(type, trans);
    }

    private static <T extends Instruction> void registerInstructionTranslator(Class<T> type, Translator<T> trans) {
        instruction_translators.put(type, trans);
    }

    private static <T extends Condition> void registerConditionTranslator(Class<T> type, Translator<T> trans) {
        condition_translators.put(type, trans);
    }

    private static interface Translator<T> {

        T translate(T val, Map<LocalInstance, LocalInstance> local_translation);

    }

    static {

        registerStatementTranslator(LocalAssignment.class, (stmt, locals) -> {
            LocalInstance mapped = locals.get(stmt.getLocal());
            return new LocalAssignment(mapped, translate(stmt.getValue(), locals));
        });

        registerInstructionTranslator(StringConstant.class, (insn, locals) -> new StringConstant(insn.getConstant()));
    }

}
