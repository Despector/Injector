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

import static com.voxelgenesis.injector.target.parse.TokenType.*;

import com.voxelgenesis.injector.target.match.InjectionMatcher;
import com.voxelgenesis.injector.target.match.InjectionModifier;
import com.voxelgenesis.injector.target.match.modifier.InstructionReplaceMatcher;
import org.spongepowered.despector.ast.type.MethodEntry;
import org.spongepowered.despector.transform.matcher.InstructionMatcher;
import org.spongepowered.despector.transform.matcher.StatementMatcher;

import java.util.ArrayList;
import java.util.List;

public class MatchParser {

    private final Lexer lexer;
    private final MethodEntry injector;

    private InjectionModifier modifier;
    private int start, end;

    public MatchParser(String str, MethodEntry mth) {
        this.lexer = new Lexer(str);
        this.injector = mth;
    }

    private void error(String msg) {
        // TODO add line and character information for debugging
        throw new IllegalStateException(msg);
    }

    private void expect(TokenType type) {
        TokenType actual = this.lexer.pop().getType();
        if (actual != type) {
            error("Expected " + type.name() + " but whats " + actual.name());
        }
    }

    public InjectionMatcher parse() {
        List<StatementMatcher<?>> matchers = new ArrayList<>();

        while (this.lexer.hasNext()) {
            matchers.add(parseStatement());
        }

        return new InjectionMatcher(matchers, this.modifier, this.start, this.end);
    }
    
    private StatementMatcher<?> parseStatement() {
        // TODO:
        // Should just parse the code to a parallel ast and write something
        // to compare asts rather than using the matchers. the replacement
        // could then just be done by walking the ast and looking for the
        // instruction marking the region being replaced.
        if (this.lexer.peekType() == IDENTIFIER) {
            ParseToken first = this.lexer.pop();
            if (this.lexer.peekType() == IDENTIFIER) {
                ParseToken name = this.lexer.pop();
                if (this.lexer.peekType() == EQUALS) {
                    this.lexer.pop();
//                    return StatementMatcher.localAssign().type(type)
                }
            }
        }

        return null;
    }

    private InstructionMatcher<?> parseInstruction() {
        if (this.lexer.peekType() == INJECTION_REPLACE) {
            this.lexer.pop();
            if (this.lexer.peekType() == INJECTION_REPLACE) {
                return new InstructionReplaceMatcher<>(null);
            } else if (this.lexer.peekType() == LEFT_PAREN) {
                InstructionMatcher<?> child = parseInstruction();
                expect(RIGHT_PAREN);
                return new InstructionReplaceMatcher<>(child);
            } else {
                error("Expected injection child");
            }
        }
        return null;
    }

}
