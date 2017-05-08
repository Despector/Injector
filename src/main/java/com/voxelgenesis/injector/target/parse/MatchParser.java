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
package com.voxelgenesis.injector.target.parse;

import com.voxelgenesis.injector.target.match.InjectionMatcher;
import com.voxelgenesis.injector.target.match.modifier.AssignmentValueModifier;
import org.spongepowered.despector.ast.generic.TypeSignature;
import org.spongepowered.despector.ast.insn.Instruction;
import org.spongepowered.despector.ast.stmt.misc.Return;
import org.spongepowered.despector.ast.type.MethodEntry;
import org.spongepowered.despector.transform.matcher.InstructionMatcher;
import org.spongepowered.despector.transform.matcher.StatementMatcher;
import org.spongepowered.despector.util.SignatureParser;

import java.util.ArrayList;
import java.util.List;

public class MatchParser {

    public static InjectionMatcher parse(String matcher, MethodEntry mth) {
        ParseState state = new ParseState(matcher);
        List<StatementMatcher<?>> matchers = new ArrayList<>();
        state.skipWhitespace();
        while (!state.isFinished()) {
            matchers.add(parseStatement(state, mth));
            state.skipWhitespace();
            state.incrementStatement();
        }
        if (state.getModifier() == null) {
            throw new IllegalStateException("No modifier found");
        }
        return new InjectionMatcher(matchers, state.getModifier(), state.getStart(), state.getEnd());
    }

    private static StatementMatcher<?> parseStatement(ParseState state, MethodEntry mth) {

        String ident = state.nextType();
        if (ident != null) {
            state.skipWhitespace();
            String name = state.nextIdentifier(ParseState.ALPHA_NUMERIC);
            if (name != null) {
                state.skipWhitespace();
                if (state.peek() == '=') {
                    state.skip(1);
                    state.skipWhitespace();
                    InstructionMatcher<?> val;
                    if (state.peek() == '$') {
                        state.skip(1);
                        state.setModifier(new AssignmentValueModifier(getValue(mth)));
                        val = InstructionMatcher.ANY;
                    } else {
                        val = parseInstruction(state, mth);
                    }
                    state.expect(';');
                    return StatementMatcher.localassign().type(getType(ident)).value(val).build();
                }
            }
        }
        state.error("Expected statement");
        return null;
    }

    private static InstructionMatcher<?> parseInstruction(ParseState state, MethodEntry mth) {
        state.error("Expected instruction");
        return null;
    }

    private static TypeSignature getType(String type) {
        return SignatureParser.parseFieldTypeSignature(type);
    }

    private static Instruction getValue(MethodEntry mth) {
        return ((Return) mth.getInstructions().get(0)).getValue().get();
    }

    private MatchParser() {
    }
}
