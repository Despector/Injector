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
import com.voxelgenesis.injector.target.match.modifier.ConditionReplaceMatcher;
import com.voxelgenesis.injector.target.match.modifier.ConditionValueModifier;
import com.voxelgenesis.injector.target.match.modifier.InstructionReplaceMatcher;
import com.voxelgenesis.injector.target.match.modifier.InstructionValueModifier;
import com.voxelgenesis.injector.target.match.modifier.StatementInsertModifier;
import org.spongepowered.despector.ast.generic.ClassTypeSignature;
import org.spongepowered.despector.ast.insn.Instruction;
import org.spongepowered.despector.ast.insn.condition.CompareCondition.CompareOperator;
import org.spongepowered.despector.ast.insn.condition.Condition;
import org.spongepowered.despector.ast.stmt.Statement;
import org.spongepowered.despector.ast.stmt.branch.If;
import org.spongepowered.despector.ast.stmt.misc.Return;
import org.spongepowered.despector.ast.type.MethodEntry;
import org.spongepowered.despector.transform.matcher.ConditionMatcher;
import org.spongepowered.despector.transform.matcher.InstructionMatcher;
import org.spongepowered.despector.transform.matcher.MatchContext;
import org.spongepowered.despector.transform.matcher.StatementMatcher;
import org.spongepowered.despector.transform.matcher.instruction.InstanceMethodInvokeMatcher;
import org.spongepowered.despector.util.ConditionUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchParser {

    private final Lexer lexer;
    private final MethodEntry injector;

    private ModifierType modifier_type;
    private int index = 0;
    private int start, end;

    private final Map<String, String> imports = new HashMap<>();

    public MatchParser(String str, MethodEntry mth) {
        this.lexer = new Lexer(str);
        this.injector = mth;

        this.imports.put("String", "Ljava/lang/String;");
    }

    public void addImport(String key, String clazz) {
        this.imports.put(key, clazz);
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
            StatementMatcher<?> next = parseStatement();
            if (next != null) {
                matchers.add(next);
            }
        }
        InjectionModifier modifier = null;
        switch (this.modifier_type) {
        case INSTRUCTION_REPLACE:
            modifier = new InstructionValueModifier(this.injector, matchers.get(this.start));
            break;
        case CONDITION_REPLACE:
            modifier = new ConditionValueModifier(this.injector, matchers.get(this.start));
            break;
        case STATEMENT_INSERT:
            modifier = new StatementInsertModifier(this.injector);
            break;
        default:
            throw new IllegalStateException();
        }

        return new InjectionMatcher(matchers, modifier, this.start, this.end);
    }

    private StatementMatcher<?> parseStatement() {
        if (this.lexer.peekType() == IDENTIFIER) {
            ParseToken first = this.lexer.pop();
            if (first.getToken().equals("if")) {
                expect(LEFT_PAREN);
                ConditionMatcher<?> condition = parseCondition();
                expect(RIGHT_PAREN);
                if (!this.lexer.hasNext()) {
                    return StatementMatcher.ifThen().condition(condition).build();
                }
                throw new IllegalStateException();
            }
            if (this.lexer.peekType() == LEFT_PAREN) {
                throw new IllegalStateException(); // TODO
            }
            String type = null;
            if (this.lexer.peekType() == TokenType.FORWARD_SLASH) {
                StringBuilder str = new StringBuilder("L").append(first.getToken());
                while (this.lexer.peekType() == FORWARD_SLASH) {
                    this.lexer.pop();
                    ParseToken next = expect(TokenType.IDENTIFIER);
                    str.append("/").append(next.getToken());
                }
                str.append(";");
                type = str.toString();
            } else {
                type = this.imports.get(first.getToken());
            }
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
            if (this.lexer.peekType() != DOT) {
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
                        int param_index = 0;
                        while (true) {
                            InstructionMatcher<?> param = parseInstruction();
                            mth.param(param_index, param);
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
        } else if (this.lexer.peekType() == INJECTION_TOKEN) {
            this.lexer.pop();
            if (this.lexer.peekType() == INJECTION_TOKEN) {
                this.lexer.pop();
                this.modifier_type = ModifierType.STATEMENT_INSERT;
                this.start = this.end = this.index;
                expect(SEMICOLON);
            }
            return null;
        }
        error("Expected statement");
        return null;
    }

    private ConditionMatcher<?> parseCondition() {
        if (this.lexer.peekType() == INJECTION_TOKEN) {
            this.lexer.pop();
            this.start = this.end = this.index;
            this.modifier_type = ModifierType.CONDITION_REPLACE;
            if (this.lexer.peekType() == LEFT_PAREN) {
                this.lexer.pop();
                ConditionMatcher<?> child = parseCondition();
                expect(RIGHT_PAREN);
                return new ConditionReplaceMatcher<>(child);
            }
            error("Expected injection child");
        }
        InstructionMatcher<?> left = parseInstruction();
        ParseToken operator = this.lexer.pop();
        InstructionMatcher<?> right = parseInstruction();
        switch (operator.getType()) {
        case NOT_EQUALS:
            return ConditionMatcher.compare().operator(CompareOperator.NOT_EQUAL).left(left).right(right).build();
        default:
            error("Invalid condition operator: " + operator.getType().name());
        }
        error("Expected condition");
        return null;
    }

    private InstructionMatcher<?> parseInstruction() {
        if (this.lexer.peekType() == INJECTION_TOKEN) {
            this.lexer.pop();
            this.start = this.end = this.index;
            this.modifier_type = ModifierType.INSTRUCTION_REPLACE;
            if (this.lexer.peekType() == INJECTION_TOKEN) {
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
            if (next.getToken().equals("null")) {
                return InstructionMatcher.nullConstant().build();
            }
            return InstructionMatcher.localAccess().allowMissing().fromContext(next.getToken()).build();
        }
        error("Expected instruction");
        return null;
    }

    private static enum ModifierType {
        INSTRUCTION_REPLACE,
        CONDITION_REPLACE,
        STATEMENT_INSERT
    }

}
