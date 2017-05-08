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
import com.voxelgenesis.injector.target.match.MatchedStatements;
import org.spongepowered.despector.ast.stmt.Statement;
import org.spongepowered.despector.ast.type.MethodEntry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InjectionTarget {

    private final String target;
    private final List<InjectionMatcher> points = new ArrayList<>();

    public InjectionTarget(String target) {
        this.target = target;
    }

    public String getTarget() {
        return this.target;
    }

    public void addInjection(InjectionMatcher point) {
        this.points.add(point);
    }

    public void apply(MethodEntry mth) {
        List<MatchedStatements> matches = new ArrayList<>();
        Set<Statement> modified = new HashSet<>();
        for (InjectionMatcher point : this.points) {
            MatchedStatements match = point.match(mth);
            if (match == null) {
                throw new IllegalStateException("No match");
            }
            for (Statement stmt : match.getModified()) {
                if (!modified.add(stmt)) {
                    throw new IllegalStateException();
                }
            }
            matches.add(match);
        }

        for (MatchedStatements match : matches) {
            match.getMatcher().apply(match);
        }

    }

}
