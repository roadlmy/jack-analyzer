package com.jack.analyzer.enumeration;

import java.util.HashSet;

public enum TokenTypeEnum {

   KEYWORD("关键词","keyword"),

   SYMBOL("标点符号","symbol"),

    STRING_CONSTANT("字符串常量","stringConstant"),

    INT_CONSTANT("数字常量","integerConstant"),

    IDENTIFIER("自定义名称","identifier");

   private String name;

   private String value;

    TokenTypeEnum(String name, String value) {
        this.name = name;
        this.value = value;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    private static final HashSet<String> keywords = new HashSet<>();

    static {
        keywords.add("class");
        keywords.add("constructor");
        keywords.add("function");
        keywords.add("method");
        keywords.add("field");
        keywords.add("static");
        keywords.add("var");
        keywords.add("int");
        keywords.add("char");
        keywords.add("boolean");
        keywords.add("void");
        keywords.add("true");
        keywords.add("false");
        keywords.add("null");
        keywords.add("this");
        keywords.add("let");
        keywords.add("do");
        keywords.add("if");
        keywords.add("else");
        keywords.add("while");
        keywords.add("return");
    }

    public static boolean isKeyword(String str) {
        return keywords.contains(str);
    }

    @Override
    public String toString() {
        return value;
    }
}
