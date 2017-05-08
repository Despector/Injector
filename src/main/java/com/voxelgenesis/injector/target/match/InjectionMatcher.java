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
package com.voxelgenesis.injector.target.match;

import org.spongepowered.despector.ast.stmt.Statement;
import org.spongepowered.despector.ast.stmt.StatementBlock;
import org.spongepowered.despector.ast.stmt.branch.DoWhile;
import org.spongepowered.despector.ast.stmt.branch.For;
import org.spongepowered.despector.ast.stmt.branch.ForEach;
import org.spongepowered.despector.ast.stmt.branch.If;
import org.spongepowered.despector.ast.stmt.branch.If.Elif;
import org.spongepowered.despector.ast.stmt.branch.Switch;
import org.spongepowered.despector.ast.stmt.branch.Switch.Case;
import org.spongepowered.despector.ast.stmt.branch.TryCatch;
import org.spongepowered.despector.ast.stmt.branch.TryCatch.CatchBlock;
import org.spongepowered.despector.ast.stmt.branch.While;
import org.spongepowered.despector.ast.type.MethodEntry;
import org.spongepowered.despector.transform.matcher.MatchContext;
import org.spongepowered.despector.transform.matcher.StatementMatcher;

import java.util.ArrayList;
import java.util.List;

public class InjectionMatcher {

    private final List<StatementMatcher<?>> matcher;
    private final InjectionModifier modifier;
    private final int start;
    private final int end;

    public InjectionMatcher(List<StatementMatcher<?>> matcher, InjectionModifier modifier, int start, int end) {
        this.matcher = matcher;
        this.modifier = modifier;
        this.start = start;
        this.end = end;
    }

    public List<StatementMatcher<?>> getMatcher() {
        return this.matcher;
    }

    public InjectionModifier getModifier() {
        return this.modifier;
    }

    public MatchedStatements match(MethodEntry mth) {
        return match(mth.getInstructions());
    }

    public MatchedStatements match(StatementBlock block) {
        MatchedStatements match = null;
        outer: for (int i = 0; i < block.size(); i++) {
            MatchContext ctx = MatchContext.create();
            match = check(block.get(i), match);
            if (i < block.size() - this.matcher.size() + 1) {
                for (int j = 0; j < this.matcher.size(); j++) {
                    Statement check = block.get(i + j);
                    StatementMatcher<?> next_matcher = this.matcher.get(j);
                    if (!next_matcher.matches(ctx, check)) {
                        continue outer;
                    }
                }
            } else {
                continue;
            }
            if (match != null) {
                throw new IllegalStateException();
            }
            List<Statement> matched = new ArrayList<>();
            for (int j = 0; j < this.matcher.size(); j++) {
                matched.add(block.get(i + j));
            }
            match = new MatchedStatements(this, block, matched);
            for (int j = this.start; j <= this.end; j++) {
                match.markModified(matched.get(j));
            }
            break;
        }
        return match;
    }

    private MatchedStatements check(Statement stmt, MatchedStatements previous) {
        MatchedStatements match = previous;
        if (stmt instanceof If) {
            If iif = (If) stmt;
            MatchedStatements pos = match(iif.getIfBody());
            if (match != null && pos != null) {
                throw new IllegalStateException();
            } else if (pos != null) {
                match = pos;
            }
            for (Elif elif : iif.getElifBlocks()) {
                pos = match(elif.getBody());
                if (match != null && pos != null) {
                    throw new IllegalStateException();
                } else if (pos != null) {
                    match = pos;
                }
            }
            pos = match(iif.getElseBlock().getElseBody());
            if (match != null && pos != null) {
                throw new IllegalStateException();
            } else if (pos != null) {
                match = pos;
            }
        } else if (stmt instanceof For) {
            For ffor = (For) stmt;
            MatchedStatements pos = match(ffor.getBody());
            if (match != null && pos != null) {
                throw new IllegalStateException();
            } else if (pos != null) {
                match = pos;
            }
        } else if (stmt instanceof ForEach) {
            ForEach ffor = (ForEach) stmt;
            MatchedStatements pos = match(ffor.getBody());
            if (match != null && pos != null) {
                throw new IllegalStateException();
            } else if (pos != null) {
                match = pos;
            }
        } else if (stmt instanceof While) {
            While wwhile = (While) stmt;
            MatchedStatements pos = match(wwhile.getBody());
            if (match != null && pos != null) {
                throw new IllegalStateException();
            } else if (pos != null) {
                match = pos;
            }
        } else if (stmt instanceof DoWhile) {
            DoWhile dowhile = (DoWhile) stmt;
            MatchedStatements pos = match(dowhile.getBody());
            if (match != null && pos != null) {
                throw new IllegalStateException();
            } else if (pos != null) {
                match = pos;
            }
        } else if (stmt instanceof TryCatch) {
            TryCatch ttry = (TryCatch) stmt;
            MatchedStatements pos = match(ttry.getTryBlock());
            if (match != null && pos != null) {
                throw new IllegalStateException();
            } else if (pos != null) {
                match = pos;
            }
            for (CatchBlock ccatch : ttry.getCatchBlocks()) {
                pos = match(ccatch.getBlock());
                if (match != null && pos != null) {
                    throw new IllegalStateException();
                } else if (pos != null) {
                    match = pos;
                }
            }
        } else if (stmt instanceof Switch) {
            Switch sswitch = (Switch) stmt;
            MatchedStatements pos = null;
            for (Case cs : sswitch.getCases()) {
                pos = match(cs.getBody());
                if (match != null && pos != null) {
                    throw new IllegalStateException();
                } else if (pos != null) {
                    match = pos;
                }
            }
        }
        return match;
    }

    public void apply(MatchedStatements mth) {
        List<Statement> statements = new ArrayList<>(mth.getStatements());
        this.modifier.apply(statements, this.start, this.end);
        List<Statement> backing = mth.getBlock().getStatements();
        int start = backing.indexOf(mth.getStatements().get(0));
        for (Statement s : mth.getStatements()) {
            backing.remove(s);
        }
        for (int i = statements.size() - 1; i >= 0; i--) {
            backing.add(start, statements.get(i));
        }
    }

}
