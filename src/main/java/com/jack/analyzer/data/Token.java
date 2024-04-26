package com.jack.analyzer.data;

import com.jack.analyzer.enumeration.TokenTypeEnum;

public class Token {

    TokenTypeEnum type;

    String value;

    Token nextToken;


    public Token() {
    }

    public Token(TokenTypeEnum type, String value, Token nextToken) {
        this.type = type;
        this.value = value;
        this.nextToken = nextToken;
    }

    public TokenTypeEnum getType() {
        return type;
    }

    public void setType(TokenTypeEnum type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Token getNextToken() {
        return nextToken;
    }

    public void setNextToken(Token nextToken) {
        this.nextToken = nextToken;
    }

    public boolean hasMoreTokens(){
        return !(nextToken == null);
    }

    public Token peek(){
        return nextToken;
    }


    @Override
    public String toString() {
        return "Token{" +
                "type=" + type +
                ", value='" + value + '\'' +
                ", nextToken=" + nextToken +
                '}';
    }

    public boolean isTypeEqualTo(TokenTypeEnum typeEnum) {
        return this.type == typeEnum;
    }

}
