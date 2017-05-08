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

import com.voxelgenesis.injector.target.match.InjectionModifier;

import java.util.BitSet;

public class ParseState {

    public static final BitSet LOWER_ALPHA;
    public static final BitSet UPPER_ALPHA;
    public static final BitSet ALPHA;
    public static final BitSet NUMERIC;
    public static final BitSet ALPHA_NUMERIC;
    public static final BitSet PRIMATIVES;
    public static final BitSet TYPE;

    private final String str;

    private int index = 0;
    private int statement = 0;
    private int start_of_statement = 0;

    private InjectionModifier modifier;
    private int start, end;

    public ParseState(String s) {
        this.str = s;
    }

    public void setModifier(InjectionModifier mod) {
        this.modifier = mod;
        this.start = this.statement;
        this.end = this.statement;
    }

    public InjectionModifier getModifier() {
        return this.modifier;
    }

    public int getStart() {
        return this.start;
    }

    public int getEnd() {
        return this.end;
    }

    public void incrementStatement() {
        this.statement++;
        this.start_of_statement = this.index;
    }

    public void error(String msg) {
        throw new IllegalStateException("Parse error at " + this.statement + ":" + (this.index - this.start_of_statement) + ": " + msg);
    }

    public void expect(char n) {
        if (peek() != n) {
            error("Expected '" + n + "' but found '" + peek() + "'");
        }
        this.index++;
    }

    public int mark() {
        return this.index;
    }

    public void reset(int index) {
        this.index = index;
    }

    public boolean isFinished() {
        return this.index >= this.str.length();
    }

    public char peek() {
        return this.str.charAt(this.index);
    }

    public void skip(int n) {
        this.index++;
    }

    public boolean skipWhitespace() {
        boolean found = false;
        while (!isFinished() && Character.isWhitespace(peek())) {
            this.index++;
            found = true;
        }
        return found;
    }

    public String nextType() {
        char n = peek();
        if (PRIMATIVES.get(n)) {
            return n + "";
        }
        expect('L');
        String val = nextIdentifier(TYPE);
        expect(';');
        return "L" + val + ";";
    }

    public String nextIdentifier(BitSet character_set) {
        char next = peek();
        StringBuilder ident = new StringBuilder();
        boolean found = false;
        while (character_set.get(next)) {
            ident.append(next);
            this.index++;
            next = peek();
            found = true;
        }
        if (!found) {
            return null;
        }
        return ident.toString();
    }

    static {
        LOWER_ALPHA = new BitSet();
        for (int i = 'a'; i <= 'z'; i++) {
            LOWER_ALPHA.set(i);
        }
        UPPER_ALPHA = new BitSet();
        for (int i = 'A'; i <= 'Z'; i++) {
            UPPER_ALPHA.set(i);
        }
        ALPHA = new BitSet();
        ALPHA.or(LOWER_ALPHA);
        ALPHA.or(UPPER_ALPHA);
        NUMERIC = new BitSet();
        for (int i = '0'; i <= '9'; i++) {
            NUMERIC.set(i);
        }
        ALPHA_NUMERIC = new BitSet();
        ALPHA_NUMERIC.or(ALPHA);
        ALPHA_NUMERIC.or(NUMERIC);
        PRIMATIVES = new BitSet();
        PRIMATIVES.set('B');
        PRIMATIVES.set('S');
        PRIMATIVES.set('I');
        PRIMATIVES.set('J');
        PRIMATIVES.set('F');
        PRIMATIVES.set('D');
        PRIMATIVES.set('Z');
        PRIMATIVES.set('C');
        TYPE = new BitSet();
        TYPE.or(ALPHA_NUMERIC);
        TYPE.set('/');
    }

}
