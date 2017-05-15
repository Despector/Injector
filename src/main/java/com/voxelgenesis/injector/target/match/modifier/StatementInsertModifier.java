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
import org.spongepowered.despector.ast.insn.condition.AndCondition;
import org.spongepowered.despector.ast.insn.condition.BooleanCondition;
import org.spongepowered.despector.ast.insn.condition.CompareCondition;
import org.spongepowered.despector.ast.insn.condition.Condition;
import org.spongepowered.despector.ast.insn.condition.OrCondition;
import org.spongepowered.despector.ast.insn.cst.NullConstant;
import org.spongepowered.despector.ast.insn.cst.StringConstant;
import org.spongepowered.despector.ast.insn.var.LocalAccess;
import org.spongepowered.despector.ast.stmt.Statement;
import org.spongepowered.despector.ast.stmt.assign.LocalAssignment;
import org.spongepowered.despector.ast.stmt.invoke.InstanceMethodInvoke;
import org.spongepowered.despector.ast.stmt.invoke.StaticMethodInvoke;
import org.spongepowered.despector.ast.type.MethodEntry;
import org.spongepowered.despector.transform.matcher.MatchContext;
import org.spongepowered.despector.util.TypeHelper;

import java.util.ArrayList;
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
        Map<LocalInstance, LocalInstance> local_translation = buildLocalTranslation(target, this.injector, match, start);

        for (int i = this.injector.getInstructions().size() - 2; i >= 0; i--) {
            Statement stmt = this.injector.getInstructions().get(i);
            statements.add(start, translate(stmt, local_translation));
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Statement> T translate(T stmt, Map<LocalInstance, LocalInstance> local_translation) {
        Translator<T> trans = (Translator<T>) statement_translators.get(stmt.getClass());
        if (trans == null) {
            throw new IllegalStateException("No translator for statement type: " + stmt.getClass().getName());
        }
        return trans.translate(stmt, local_translation);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Instruction> T translate(T stmt, Map<LocalInstance, LocalInstance> local_translation) {
        Translator<T> trans = (Translator<T>) instruction_translators.get(stmt.getClass());
        if (trans == null) {
            throw new IllegalStateException("No translator for instruction type: " + stmt.getClass().getName());
        }
        return trans.translate(stmt, local_translation);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Condition> T translate(T stmt, Map<LocalInstance, LocalInstance> local_translation) {
        Translator<T> trans = (Translator<T>) condition_translators.get(stmt.getClass());
        if (trans == null) {
            throw new IllegalStateException("No translator for condition type: " + stmt.getClass().getName());
        }
        return trans.translate(stmt, local_translation);
    }

    public static Map<LocalInstance, LocalInstance> buildLocalTranslation(MethodEntry target, MethodEntry injector, MatchContext match, int offs) {
        Map<LocalInstance, LocalInstance> local_translation = new HashMap<>();
        int local_index = 0;
        if (!injector.isStatic()) {
            LocalInstance inj_this = injector.getLocals().getLocal(0).getParameterInstance();
            LocalInstance target_this = target.getLocals().getLocal(0).getParameterInstance();
            local_translation.put(inj_this, target_this);
            local_index++;
        }
        for (String p : TypeHelper.splitSig(injector.getDescription())) {
            LocalInstance inj_param = injector.getLocals().getLocal(local_index).getParameterInstance();
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
        for (LocalInstance instance : injector.getLocals().getAllInstances()) {
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

        registerInstructionTranslator(InstanceMethodInvoke.class, (insn, locals) -> {
            Instruction[] args = new Instruction[insn.getParameters().length];
            for (int i = 0; i < insn.getParameters().length; i++) {
                args[i] = translate(insn.getParameters()[i], locals);
            }
            Instruction callee = translate(insn.getCallee(), locals);
            return new InstanceMethodInvoke(insn.getType(), insn.getMethodName(), insn.getMethodDescription(), insn.getOwner(), args, callee);
        });
        registerInstructionTranslator(LocalAccess.class, (insn, locals) -> {
            LocalInstance mapped = locals.get(insn.getLocal());
            return new LocalAccess(mapped);
        });
        registerInstructionTranslator(NullConstant.class, (insn, locals) -> NullConstant.NULL);
        registerInstructionTranslator(StaticMethodInvoke.class, (insn, locals) -> {
            Instruction[] args = new Instruction[insn.getParameters().length];
            for (int i = 0; i < insn.getParameters().length; i++) {
                args[i] = translate(insn.getParameters()[i], locals);
            }
            return new StaticMethodInvoke(insn.getMethodName(), insn.getMethodDescription(), insn.getOwner(), args);
        });
        registerInstructionTranslator(StringConstant.class, (insn, locals) -> new StringConstant(insn.getConstant()));

        registerConditionTranslator(AndCondition.class, (cond, locals) -> {
            List<Condition> ops = new ArrayList<>();
            for (Condition op : cond.getOperands()) {
                ops.add(translate(op, locals));
            }
            return new AndCondition(ops);
        });
        registerConditionTranslator(BooleanCondition.class, (cond, locals) -> {
            Instruction val = translate(cond.getConditionValue(), locals);
            return new BooleanCondition(val, cond.isInverse());
        });
        registerConditionTranslator(CompareCondition.class, (cond, locals) -> {
            Instruction left = translate(cond.getLeft(), locals);
            Instruction right = translate(cond.getRight(), locals);
            return new CompareCondition(left, right, cond.getOperator());
        });
        registerConditionTranslator(OrCondition.class, (cond, locals) -> {
            List<Condition> ops = new ArrayList<>();
            for (Condition op : cond.getOperands()) {
                ops.add(translate(op, locals));
            }
            return new OrCondition(ops);
        });
    }

}
