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
import org.spongepowered.despector.ast.Locals.LocalInstance;
import org.spongepowered.despector.ast.insn.condition.Condition;
import org.spongepowered.despector.ast.stmt.Statement;
import org.spongepowered.despector.ast.stmt.assign.LocalAssignment;
import org.spongepowered.despector.ast.stmt.branch.If;
import org.spongepowered.despector.ast.type.MethodEntry;
import org.spongepowered.despector.transform.matcher.ConditionMatcher;
import org.spongepowered.despector.transform.matcher.InstructionMatcher;
import org.spongepowered.despector.transform.matcher.MatchContext;
import org.spongepowered.despector.transform.matcher.StatementMatcher;
import org.spongepowered.despector.transform.matcher.statement.IfMatcher;
import org.spongepowered.despector.util.ConditionUtil;

import java.util.List;
import java.util.Map;

public class ConditionValueModifier implements InjectionModifier {

    private static final StatementMatcher<?> RETURN_TRUE = StatementMatcher.returnValue()
            .value(InstructionMatcher.intConstant()
                    .value(1)
                    .build())
            .build();

    private final MethodEntry replacement;
    private final StatementMatcher<?> matcher;

    public ConditionValueModifier(MethodEntry replacement, StatementMatcher<?> matcher) {
        this.replacement = replacement;
        this.matcher = matcher;
    }

    @Override
    public void apply(List<Statement> statements, int start, int end, MethodEntry target, MatchContext match) {
        Map<LocalInstance, LocalInstance> local_translation = StatementInsertModifier.buildLocalTranslation(target, this.replacement, match, start);
        replaceInStatement(statements.get(start), this.matcher, StatementInsertModifier.translate(getConditionReplace(), local_translation));
    }

    private Condition getConditionReplace() {
        Statement s = this.replacement.getInstructions().get(0);
        if (s instanceof If) {
            If iif = (If) s;
            if (RETURN_TRUE.matches(MatchContext.create(), iif.getBody().get(0))) {
                return iif.getCondition();
            }
            return ConditionUtil.inverse(iif.getCondition());
        }
        throw new IllegalStateException();
    }

    public static void replaceInStatement(Statement value, StatementMatcher<?> root_matcher, Condition replacement) {
        if (root_matcher instanceof MatchContext.LocalStoreMatcher) {
            replaceInStatement(value, ((MatchContext.LocalStoreMatcher<?>) root_matcher).getInternalMatcher(), replacement);
        } else if(value instanceof If) {
            IfMatcher matcher = (IfMatcher) root_matcher;
            If iif = (If) value;
            iif.setCondition(replaceInCondition(iif.getCondition(), matcher.getConditionMatcher(), replacement));
        } else if (value instanceof LocalAssignment) {
        } else {
            throw new IllegalStateException("Unsupported matcher " + root_matcher.getClass().getName());
        }
    }

    public static Condition replaceInCondition(Condition value, ConditionMatcher<?> root_matcher, Condition replacement) {
        if (root_matcher instanceof ConditionReplaceMatcher) {
            return replacement;
        }
        throw new IllegalStateException("Unsupported matcher " + root_matcher.getClass().getName());
    }

}
