package com.jack.analyzer;

import com.jack.analyzer.data.Symbol;
import com.jack.analyzer.enumeration.VarTypeEnum;

import java.util.HashMap;

public class SymbolTable {

    private HashMap<String, Symbol> classSymbolMap;

    private int classFieldNum;

    private int classStaticNum;

    private int subArgNum;

    private int subLocalNum;
    private HashMap<String, Symbol> subroutineSymbolMap;

    public SymbolTable() {
        this.classSymbolMap = new HashMap<>();
        this.classFieldNum = 0;
        this.classStaticNum = 0;
    }

    public void define(String name, String type, String kind){
        if(kind.equals("static") && (classSymbolMap.get(name) == null)){
                classSymbolMap.put(name,new Symbol(name,type,kind,String.valueOf(this.classStaticNum)));
                classStaticNum++;
        }else if(kind.equals("field") && (classSymbolMap.get(name) == null)){
            // this指针所有的意义，就是取到当前对象object的field字段对应的值（在heap里）
            classSymbolMap.put(name,new Symbol(name,type,"this",String.valueOf(this.classFieldNum)));
            classFieldNum++;
        }else if(kind.equals("argument") && (classSymbolMap.get(name) == null)){
            subroutineSymbolMap.put(name,new Symbol(name,type,kind,String.valueOf(this.subArgNum)));
            subArgNum++;
        }else if(kind.equals("local") && (classSymbolMap.get(name) == null)){
            subroutineSymbolMap.put(name,new Symbol(name,type,kind,String.valueOf(this.subLocalNum)));
            subLocalNum++;
        }else {
            throw new RuntimeException("没有找到这个类型的变量，只支持static/field/argument/local");
        }
    }

    public void startSubroutine() {
        this.subroutineSymbolMap = new HashMap<>();
        this.subArgNum = 0;
        this.subLocalNum = 0;
    }

    public String kindOf(String name){
        if(subroutineSymbolMap != null) {
            Symbol symbol = subroutineSymbolMap.get(name);
            if (symbol != null) {
                return symbol.getKind();
            }else {
                if(classSymbolMap != null){
                    Symbol symbol1 = classSymbolMap.get(name);
                    if(symbol1 != null){
                        return symbol1.getKind();
                    }else {
                        throw new RuntimeException("变量为定义");
                    }
                }
            }
        }
        return null;
    }

    public String typeOf(String name){
        if(subroutineSymbolMap != null) {
            Symbol symbol = subroutineSymbolMap.get(name);
            if (symbol != null) {
                return symbol.getType();
            }else {
                if(classSymbolMap != null){
                    Symbol symbol1 = classSymbolMap.get(name);
                    if(symbol1 != null){
                        return symbol1.getType();
                    }else {
                        throw new RuntimeException("变量为定义");
                    }
                }
            }
        }
        return null;
    }

    public String indexOf(String name){
        if(subroutineSymbolMap != null) {
            Symbol symbol = subroutineSymbolMap.get(name);
            if (symbol != null) {
                return symbol.getIndex();
            }else {
                if(classSymbolMap != null){
                    Symbol symbol1 = classSymbolMap.get(name);
                    if(symbol1 != null){
                        return symbol1.getIndex();
                    }else {
                        throw new RuntimeException("变量未定义");
                    }
                }
            }
        }
        return null;
    }

    public int getClassFieldNum() {

        return this.classFieldNum;
    }

    public int getClassStaticNum() {
        return classStaticNum;
    }

    public int getSubArgNum() {
        return subArgNum;
    }

    public int getSubLocalNum() {
        return subLocalNum;
    }
}
