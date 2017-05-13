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
import com.voxelgenesis.injector.target.match.modifier.AssignmentValueModifier;
import com.voxelgenesis.injector.target.match.modifier.InstructionReplaceMatcher;
import org.spongepowered.despector.ast.generic.ClassTypeSignature;
import org.spongepowered.despector.ast.insn.Instruction;
import org.spongepowered.despector.ast.stmt.misc.Return;
import org.spongepowered.despector.ast.type.MethodEntry;
import org.spongepowered.despector.transform.matcher.InstructionMatcher;
import org.spongepowered.despector.transform.matcher.MatchContext;
import org.spongepowered.despector.transform.matcher.StatementMatcher;
import org.spongepowered.despector.transform.matcher.instruction.InstanceMethodInvokeMatcher;

import java.util.ArrayList;
import java.util.List;

public class MatchParser {

    private final Lexer lexer;
    private final MethodEntry injector;

    private ModifierType modifier_type;
    private int index = 0;
    private int start, end;

    public MatchParser(String str, MethodEntry mth) {
        this.lexer = new Lexer(str);
        this.injector = mth;
    }

    private void error(String msg) {
        // TODO add line and character information for debugging
        throw new IllegalStateException(msg);
    }

    private ParseToken expect(TokenType type) {
        ParseToken actual = this.lexer.pop();
        if (actual.getType() != type) {
            error("Expected " + type.name() + " but whats " + actual.getType().name());
        }
        return actual;
    }

    public InjectionMatcher parse() {
        List<StatementMatcher<?>> matchers = new ArrayList<>();

        while (this.lexer.hasNext()) {
            this.index = matchers.size();
            matchers.add(parseStatement());
        }
        InjectionModifier modifier = null;
        if (this.modifier_type == ModifierType.INSTRUCTION_REPLACE) {
            modifier = new AssignmentValueModifier(getValueReplace(), matchers.get(this.start));
        }

        return new InjectionMatcher(matchers, modifier, this.start, this.end);
    }

    private Instruction getValueReplace() {
        return ((Return) this.injector.getInstructions().get(0)).getValue().get();
    }

    private StatementMatcher<?> parseStatement() {
        if (this.lexer.peekType() == IDENTIFIER) {
            ParseToken first = this.lexer.pop();
            if (this.lexer.peekType() == LEFT_PAREN) {
                throw new IllegalStateException(); // TODO
            }
            StringBuilder type = new StringBuilder("L").append(first.getToken());
            while (this.lexer.peekType() == FORWARD_SLASH) {
                this.lexer.pop();
                ParseToken next = expect(TokenType.IDENTIFIER);
                type.append("/").append(next.getToken());
            }
            type.append(";");
            if (this.lexer.peekType() == IDENTIFIER) {
                ParseToken name = this.lexer.pop();
                if (this.lexer.peekType() == EQUALS) {
                    this.lexer.pop();
                    InstructionMatcher<?> val = parseInstruction();
                    expect(SEMICOLON);
                    return MatchContext.storeLocal(name.getToken(),
                            StatementMatcher.localAssign().type(ClassTypeSignature.of(type.toString())).value(val).build());
                }
                throw new IllegalStateException();
            }
            InstructionMatcher<?> owner = null;
            while (this.lexer.peekType() == DOT) {
                this.lexer.pop();
                ParseToken next = expect(IDENTIFIER);
                if (this.lexer.peekType() == DOT) {
                    if (owner == null) {
                        owner = InstructionMatcher.staticFieldAccess().owner(ClassTypeSignature.of(type.toString())).name(next.getToken()).build();
                    } else {
                        owner = InstructionMatcher.instanceFieldAccess().owner(owner).name(next.getToken()).build();
                    }
                } else if (this.lexer.peekType() == LEFT_PAREN) {
                    this.lexer.pop();
                    InstanceMethodInvokeMatcher.Builder mth = InstructionMatcher.instanceMethodInvoke().callee(owner).name(next.getToken());
                    if (this.lexer.peekType() != RIGHT_PAREN) {
                        while (true) {
                            InstructionMatcher<?> param = parseInstruction();
                            if (this.lexer.peekType() == RIGHT_PAREN) {
                                break;
                            }
                            expect(COMMA);
                        }
                    }
                    expect(RIGHT_PAREN);
                    owner = mth.build();
                }
            }
            expect(SEMICOLON);
            return StatementMatcher.invoke().value(owner).build();
        }
        error("Expected statement");
        return null;
    }

    private InstructionMatcher<?> parseInstruction() {
        if (this.lexer.peekType() == INJECTION_REPLACE) {
            this.lexer.pop();
            this.start = this.end = this.index;
            this.modifier_type = ModifierType.INSTRUCTION_REPLACE;
            if (this.lexer.peekType() == INJECTION_REPLACE) {
                return new InstructionReplaceMatcher<>(null);
            } else if (this.lexer.peekType() == LEFT_PAREN) {
                this.lexer.pop();
                InstructionMatcher<?> child = parseInstruction();
                expect(RIGHT_PAREN);
                return new InstructionReplaceMatcher<>(child);
            }
            error("Expected injection child");
        }
        if (this.lexer.peekType() == STRING_CONSTANT) {
            ParseToken next = this.lexer.pop();
            return InstructionMatcher.stringConstant().value(next.getToken()).build();
        }
        if (this.lexer.peekType() == IDENTIFIER) {
            ParseToken next = this.lexer.pop();
            return InstructionMatcher.localAccess().fromContext(next.getToken()).build();
        }
        error("Expected instruction");
        return null;
    }

    private static enum ModifierType {
        INSTRUCTION_REPLACE
    }

}
