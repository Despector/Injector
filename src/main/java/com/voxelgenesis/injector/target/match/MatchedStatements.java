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
import org.spongepowered.despector.transform.matcher.MatchContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MatchedStatements {

    private final InjectionMatcher point;
    private final MatchContext ctx;
    private final StatementBlock block;
    private final List<Statement> matched = new ArrayList<>();
    private final List<Statement> modified = new ArrayList<>();

    public MatchedStatements(InjectionMatcher point, MatchContext ctx, StatementBlock block, Collection<Statement> stmt) {
        this.point = point;
        this.ctx = ctx;
        this.block = block;
        this.matched.addAll(stmt);
    }

    public InjectionMatcher getMatcher() {
        return this.point;
    }

    public MatchContext getMatchContext() {
        return this.ctx;
    }

    public StatementBlock getBlock() {
        return this.block;
    }

    public List<Statement> getStatements() {
        return this.matched;
    }

    public void markModified(Statement stmt) {
        this.modified.add(stmt);
    }

    public List<Statement> getModified() {
        return this.modified;
    }

}
