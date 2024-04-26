package com.jack.analyzer.enumeration;

import java.util.HashSet;

public enum VarTypeEnum {

   STATIC("关键词","static"),

   FIELD("标点符号","field"),

    ARG("字符串常量","argument"),

    VAR("数字常量","local");

   private String name;

   private String value;

    VarTypeEnum(String name, String value) {
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

    @Override
    public String toString() {
        return value;
    }
}
