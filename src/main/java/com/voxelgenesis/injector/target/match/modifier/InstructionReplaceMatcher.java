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

import org.spongepowered.despector.ast.insn.Instruction;
import org.spongepowered.despector.ast.stmt.Statement;
import org.spongepowered.despector.ast.stmt.assign.LocalAssignment;
import org.spongepowered.despector.transform.matcher.InstructionMatcher;
import org.spongepowered.despector.transform.matcher.MatchContext;
import org.spongepowered.despector.transform.matcher.StatementMatcher;
import org.spongepowered.despector.transform.matcher.instruction.IntConstantMatcher;
import org.spongepowered.despector.transform.matcher.instruction.StringConstantMatcher;
import org.spongepowered.despector.transform.matcher.statement.LocalAssignmentMatcher;

public class InstructionReplaceMatcher<T extends Instruction> implements InstructionMatcher<T> {

    private final InstructionMatcher<T> child;

    public InstructionReplaceMatcher(InstructionMatcher<T> child) {
        this.child = child;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T match(MatchContext ctx, Instruction insn) {
        return this.child == null ? (T) insn : this.child.match(insn);
    }

    public static void replaceInStatement(Statement value, StatementMatcher<?> root_matcher, Instruction replacement) {
        if (root_matcher instanceof MatchContext.LocalStoreMatcher) {
            replaceInStatement(value, ((MatchContext.LocalStoreMatcher<?>) root_matcher).getInternalMatcher(), replacement);
        } else if (value instanceof LocalAssignment) {
            LocalAssignmentMatcher match = (LocalAssignmentMatcher) root_matcher;
            LocalAssignment assign = (LocalAssignment) value;
            assign.setValue(replaceInValue(assign.getValue(), match.getValueMatcher(), replacement));
        } else {
            throw new IllegalStateException("Unsupported matcher " + root_matcher.getClass().getName());
        }
    }

    public static Instruction replaceInValue(Instruction value, InstructionMatcher<?> root_matcher, Instruction replacement) {
        if (root_matcher instanceof InstructionReplaceMatcher) {
            return replacement;
        } else if (root_matcher instanceof StringConstantMatcher || root_matcher instanceof IntConstantMatcher) {
            return value;
        }
        throw new IllegalStateException("Unsupported matcher " + root_matcher.getClass().getName());
    }

}
