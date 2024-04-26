package com.jack.analyzer.data;

import com.jack.analyzer.enumeration.VarTypeEnum;

public class Symbol {

    private String name;

    private String  type;

    private String kind;

    private String index;

    public Symbol(String name, String type, String kind, String index) {
        this.name = name;
        this.type = type;
        this.kind = kind;
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }
}
