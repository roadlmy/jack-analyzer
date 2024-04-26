package com.jack.analyzer;

import com.jack.analyzer.data.IntegerWrapper;
import com.jack.analyzer.data.Token;
import com.jack.analyzer.enumeration.TokenTypeEnum;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public class CompileEngine {

    Token currentToken;
    
    File fileOutAnalyzer;

    File vmOut;

    BufferedWriter bw;

    VMWriter vmWriter;

    HashMap<String,Integer> currentFunctionWhileNum = new HashMap<>();

    private int labelIndex;

    public CompileEngine(Token currentToken, File fileOutAnalyzer, File vmOut) throws IOException {
        this.currentToken = currentToken;
        this.fileOutAnalyzer = fileOutAnalyzer;
        this.vmOut = vmOut;
        this.bw = new BufferedWriter(new FileWriter(fileOutAnalyzer));
        this.vmWriter = new VMWriter(vmOut);
        this.labelIndex = 0;
    }


    public void analyze() throws IOException {
        if (currentToken.hasMoreTokens()) {
            currentToken = currentToken.getNextToken();
            while (currentToken.hasMoreTokens()) {
                // deal with current token
                if (currentToken.getValue().equals("class")) {
                    compileClass();
                }

                //getNextToken
                currentToken = currentToken.getNextToken();
                if(currentToken == null){
                    break;
                }
            }
        }
        this.vmWriter.close();

        bw.flush();
        bw.close();
    }

    private void compileClass() throws IOException {
        // class:'class' className '{' classVarDec* subroutineDec* '}'
        bw.write("<class>\n");
        bw.write(process("keyword", "class")+"\n");
        String className = currentToken.getValue();
        bw.write(process("identifier", currentToken.getValue())+"\n");
        bw.write(process("symbol", "{")+"\n");

        // 类级别：新建一个SymbolTable
        SymbolTable symbolTable = new SymbolTable();

        while(currentToken.hasMoreTokens()){
            if(currentToken.getValue().equals("static") || currentToken.getValue().equals("field")){
                compileClassVarDec(symbolTable);
            }

            if(currentToken.getValue().equals("constructor") || currentToken.getValue().equals("function") || currentToken.getValue().equals("method")){
                compileSubroutineDec(symbolTable,className);
            }

        }
        bw.write(process("symbol", "}")+"\n");

        bw.write("</class>\n");

    }

    private void compileSubroutineDec(SymbolTable symbolTable,String className) throws IOException {
        // 'constructor'|'function'|'method' 'void'|type subroutineName '(' parameterList ')' subroutinebody

        // start a new subrountine table
        symbolTable.startSubroutine();
        bw.write("<subroutineDec>"+"\n");
        String functionType = currentToken.getValue(); // constructor/function/method
        bw.write(process("keyword", "constructor,function,method")+"\n");
        if(functionType.equals("method")){
            symbolTable.define("this",className,"argument");
        }
        bw.write(process("identifier,keyword", currentToken.getValue())+"\n");
        String name = currentToken.getValue();
        bw.write(process("identifier", currentToken.getValue())+"\n");
        bw.write(process("symbol", "(")+"\n");
        bw.write("<parameterList>"+"\n");
        IntegerWrapper argsNum = new IntegerWrapper(0);
        if(!currentToken.getValue().equals(")")){
            increaseValue(argsNum);
            compileParameterList(symbolTable,argsNum);
        }

        bw.write("</parameterList>"+"\n");
        bw.write(process("symbol", ")")+"\n");
        compileSubroutineBody(symbolTable,className,functionType,argsNum,name);
        bw.write("</subroutineDec>"+"\n");
    }

    private void compileSubroutineBody(SymbolTable symbolTable, String className, String functionType, IntegerWrapper argsNum, String name) throws IOException {
        // '{' varDec* statements '}'
        this.currentFunctionWhileNum.put(className+"While",0);
        this.currentFunctionWhileNum.put(className+"Let",0);

        bw.write("<subroutineBody>"+"\n");
        bw.write(process("symbol", "{")+"\n");
        while (currentToken.getValue().equals("var")) {
            compileVarDec(symbolTable);
        }
        switch (functionType) {
            case "method" -> {
                increaseValue(argsNum);
                vmWriter.writeFunction(className + "." + name, String.valueOf(symbolTable.getSubLocalNum())); // function Foo.bar localsNum 不用加一，arg会比lcl多1

                vmWriter.writePush("argument", String.valueOf(0)); // push argument 0

                vmWriter.writePop("pointer", String.valueOf(0)); // pop pointer 0
            }
            case "function" ->
                // static方法是不需要用this指向当前对象的，因为在对象生成之前就可以调用静态方法
                    vmWriter.writeFunction(className + "." + name, String.valueOf(symbolTable.getSubLocalNum()));
            case "constructor" -> {
                vmWriter.writeFunction(className + "." + name, String.valueOf(symbolTable.getSubLocalNum()));// function Point.new localsNum

                vmWriter.writePush("constant", String.valueOf(symbolTable.getClassFieldNum())); // push constant argsNum

                vmWriter.writeCall("Memory.alloc", String.valueOf(1)); // call Memory.alloc 1

                vmWriter.writePop("pointer", String.valueOf(0)); // pop pointer 0
            }
        }
        compileStatements(symbolTable,className);
        bw.write(process("symbol", "}")+"\n");
        bw.write("</subroutineBody>"+"\n");
    }

    private void compileStatements(SymbolTable symbolTable,String className) throws IOException {
        bw.write("<statements>"+"\n");
        
        while(currentToken.hasMoreTokens()){
            if(currentToken.getValue().equals("let")){
                compileLet(symbolTable, className);
                continue;
            }
            if(currentToken.getValue().equals("do")){
                compileDo(symbolTable, className);
                continue;
            }
            if(currentToken.getValue().equals("if")){
                compileIf(symbolTable,className);
                continue;
            }
            if(currentToken.getValue().equals("while")){
                compileWhile(symbolTable, className);
                continue;
            }
            if(currentToken.getValue().equals("return")){
                compileReturn(symbolTable, className);
                continue;
            }
            if(currentToken.getValue().equals("}")) {
                break;
            }

            throw new RuntimeException("当前的subroutine keyword："+currentToken.getValue() + "不存在，请检查源码");
        }

        bw.write("</statements>"+"\n");
        
    }

    private void compileReturn(SymbolTable symbolTable, String className) throws IOException {
        // 'return' expression? ';'
        bw.write("<returnStatement>"+"\n");
        bw.write(process("keyword", "return")+"\n");

        if(!currentToken.getValue().equals(";")){
            compileExpression(symbolTable, className);
        }else {
            vmWriter.writePush("constant","0");
        }

        vmWriter.writeReturn();
        bw.write(process("symbol", ";")+"\n");

        bw.write("</returnStatement>"+"\n");

    }

    private void compileExpression(SymbolTable symbolTable, String className) throws IOException {
        // term (op term)*
        // term: integerConstant | stringConstant | keywordConstant | varName |
        // varName'['expression']'| subroutineCall | '('expression')'|unaryOp term
        // subroutineCall: subroutineName '(' expressionList ')' | (className|varName)'.'subroutineName '(' expressionList ')'
        // expressionList: (expression (','expression)*)?
        // op: '+'|'-'|'*'|'/'|'&'|'|'|'<'|'>'|'='
        // unaryOp: '-'|'~'
        // keywordConstant: 'true'|'false'|'null'|'this'
        bw.write("<expression>"+"\n");

        compileTerm(symbolTable,className);
        while (isOperator(currentToken.getValue())){
            String op = currentToken.getValue();
            bw.write(process("symbol", currentToken.getValue())+"\n");
            compileTerm(symbolTable,className);
            vmWriter.writeArithmetic(op);
        }
        bw.write("</expression>"+"\n");

    }

    private void compileTerm(SymbolTable symbolTable, String className) throws IOException {
        String op = null;
        bw.write("<term>"+"\n");
        // ** -3 一定要先于identifier判断，不然会遇到size - 2 size被pop掉之后也是-2的情况
        if(isUnaryOp(currentToken.getValue())){
            op = currentToken.getValue();
            bw.write(process("symbol", currentToken.getValue())+"\n");
            compileTerm(symbolTable, className);
            vmWriter.writeUnaryArithmetic(op);
        }
        if(currentToken.isTypeEqualTo(TokenTypeEnum.INT_CONSTANT) || currentToken.isTypeEqualTo(TokenTypeEnum.STRING_CONSTANT) || isKeywordConstant(currentToken.getValue())){
            String value = currentToken.getValue();
            if(currentToken.isTypeEqualTo(TokenTypeEnum.INT_CONSTANT)){
                vmWriter.writePush("constant",value);
            }else if(isKeywordConstant(currentToken.getValue()) && !currentToken.getValue().equals("this")){
                if(currentToken.getValue().equals("true")){
                    vmWriter.writePush("constant",String.valueOf(0));
                    vmWriter.writeUnaryArithmetic("~");
                }else {
                    vmWriter.writePush("constant",keywordConstantMap(currentToken.getValue()));
                }
            }else if(currentToken.getValue().equals("this")){
                vmWriter.writePush("pointer","0");
            }else if(currentToken.isTypeEqualTo(TokenTypeEnum.STRING_CONSTANT)){
                String str = currentToken.getValue();
                Integer len = str.length();
                vmWriter.writePush("constant",String.valueOf(len));
                vmWriter.writeCall("String.new",String.valueOf(1));
                for(int i =0;i<len;i++){
                    int c = str.charAt(i);
                    vmWriter.writePush("constant", String.valueOf(c));
                    vmWriter.writeCall("String.appendChar",String.valueOf(2));
                }
            }

            bw.write(process("integerConstant,stringConstant,keyword", currentToken.getValue())+"\n");

        }

        if(currentToken.isTypeEqualTo(TokenTypeEnum.IDENTIFIER)){
            switch (currentToken.getNextToken().getValue()) {
                case "[" -> {
                    // foo[expression]
                    // 处理array foo+expression
                    vmWriter.writePush(symbolTable.kindOf(currentToken.getValue()), symbolTable.indexOf(currentToken.getValue()));

                    bw.write(process("identifier", currentToken.getValue()) + "\n");
                    bw.write(process("symbol", "[") + "\n");
                    compileExpression(symbolTable, className);
                    vmWriter.writeArithmetic("+");
                    vmWriter.writePop("pointer", String.valueOf(1));
                    vmWriter.writePush("that", String.valueOf(0));

                    bw.write(process("symbol", "]") + "\n");
                }
                case "." -> {
                    // foo.bar(expressionList) | Foo.bar(expressionList) 或者构造方法 Point.new(expressionList)
                    String name = currentToken.getValue();
                    IntegerWrapper argsNum = new IntegerWrapper(0);
                    if (!Character.isUpperCase(currentToken.getValue().charAt(0))) {
                        // foo.bar要传入this作为第一个argument
                        argsNum = new IntegerWrapper(1);
                        vmWriter.writePush(symbolTable.kindOf(currentToken.getValue()), symbolTable.indexOf(currentToken.getValue()));
                        name = symbolTable.typeOf(currentToken.getValue()) + "." + currentToken.getNextToken().getNextToken().getValue();
                    } else {
                        name = name + "." + currentToken.getNextToken().getNextToken().getValue();
                    }

                    bw.write(process("identifier", currentToken.getValue()) + "\n");
                    bw.write(process("symbol", ".") + "\n");
                    bw.write(process("identifier", currentToken.getValue()) + "\n");
                    bw.write(process("symbol", "(") + "\n");
                    compileExpressionList(symbolTable, argsNum, className);
                    bw.write(process("symbol", ")") + "\n");
                    vmWriter.writeCall(name, String.valueOf(argsNum));

                }
                case "(" -> {
                    // foo(expressionList)
                    // 此时需要传this，一视同仁！ do foo()的话 就是 push pointer 0
                    String name = currentToken.getValue();
                    IntegerWrapper argsNum = new IntegerWrapper(1);
                    vmWriter.writePush("pointer", String.valueOf(0));

                    bw.write(process("identifier", currentToken.getValue()) + "\n");
                    bw.write(process("symbol", "(") + "\n");
                    compileExpressionList(symbolTable, argsNum, className);
                    bw.write(process("symbol", ")") + "\n");
                    vmWriter.writeCall(className + "." + name, String.valueOf(argsNum));
                }
                default -> {
                    // foo
                    // 只有一个变量
                    vmWriter.writePush(symbolTable.kindOf(currentToken.getValue()), symbolTable.indexOf(currentToken.getValue()));
                    bw.write(process("identifier", currentToken.getValue()) + "\n");
                }
            }
        }
        if(currentToken.getValue().equals("(")){
            bw.write(process("symbol", "(")+"\n");
            compileExpression(symbolTable, className);
            bw.write(process("symbol", ")")+"\n");
        }
        bw.write("</term>"+"\n");
    }

    private void compileExpressionList(SymbolTable symbolTable, IntegerWrapper argsNum, String className) throws IOException {
        // (expression (','expression)*)?
        bw.write("<expressionList>"+"\n");

        if(!currentToken.getValue().equals(")")){
            increaseValue(argsNum);
            compileExpression(symbolTable, className);
            while (currentToken.getValue().equals(",")) {
                bw.write(process("symbol", ",")+"\n");
                increaseValue(argsNum);
                compileExpression(symbolTable, className);
            }
        }

        bw.write("</expressionList>"+"\n");

    }

    private String newLabel(){
        return "LABEL_" + (labelIndex++);
    }
    private void compileWhile(SymbolTable symbolTable,String className) throws IOException {
        String continueLabel = newLabel();
        String topLabel = newLabel();

        vmWriter.writeLabel(topLabel);
        // 'while' '(' expression ')' '{' statements '}'
        bw.write("<whileStatement>"+"\n");
        bw.write(process("keyword", "while")+"\n");
        bw.write(process("symbol", "(")+"\n");
        compileExpression(symbolTable, className);

        vmWriter.writeUnaryArithmetic("~");
        vmWriter.writeIf(continueLabel);
        bw.write(process("symbol", ")")+"\n");
        bw.write(process("symbol", "{")+"\n");
        compileStatements(symbolTable,className);
        vmWriter.writeGoto(topLabel);
        vmWriter.writeLabel(continueLabel);

        bw.write(process("symbol", "}")+"\n");


        bw.write("</whileStatement>"+"\n");

    }
    private void compileIf(SymbolTable symbolTable, String className) throws IOException {

        String elseLabel = newLabel();
        String endLabel = newLabel();

        // 'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}')?
        bw.write("<ifStatement>"+"\n");
        bw.write(process("keyword", "if")+"\n");

        bw.write(process("symbol", "(")+"\n");

        compileExpression(symbolTable, className);
        vmWriter.writeUnaryArithmetic("~");
        vmWriter.writeIf(elseLabel);

        bw.write(process("symbol", ")")+"\n");
        bw.write(process("symbol", "{")+"\n");
        compileStatements(symbolTable,className);
        bw.write(process("symbol", "}")+"\n");
        vmWriter.writeGoto(endLabel);

        vmWriter.writeLabel(elseLabel);
        if(currentToken.getValue().equals("else")){
            bw.write(process("keyword", "else")+"\n");
            bw.write(process("symbol", "{")+"\n");
            compileStatements(symbolTable,className);
            bw.write(process("symbol", "}")+"\n");
        }

        vmWriter.writeLabel(endLabel);


        bw.write("</ifStatement>"+"\n");
    }
    private void compileDo(SymbolTable symbolTable, String className) throws IOException {
        // 'do' subroutineCall ';'
        // subroutineCall: subroutineName '(' expressionList ')' | (className|varName)'.'subroutineName '(' expressionList ')'
        // subroutineName: identifier
        bw.write("<doStatement>"+"\n");
        bw.write(process("keyword", "do")+"\n");

        // compileSubroutineCall;
        if(currentToken.getNextToken().getValue().equals("(")){
            // foo(expressionList)
            String name = currentToken.getValue();
            IntegerWrapper argsNum = new IntegerWrapper(1);
            vmWriter.writePush("pointer",String.valueOf(0));
            bw.write(process("identifier", currentToken.getValue())+"\n");
            bw.write(process("symbol", "(")+"\n");

            compileExpressionList(symbolTable,argsNum, className);
            bw.write(process("symbol", ")")+"\n");
            vmWriter.writeCall( className + "." + name,String.valueOf(argsNum));

        }else {
            // foo.bar(expressionList) | Foo.bar(expressionList)
            String name = currentToken.getValue();
            IntegerWrapper argsNum = new IntegerWrapper(0);
            if(!Character.isUpperCase(currentToken.getValue().charAt(0))){
                // foo.bar要传入this作为第一个argument
                argsNum = new IntegerWrapper(1);
                vmWriter.writePush(symbolTable.kindOf(currentToken.getValue()),symbolTable.indexOf(currentToken.getValue()));
                name = symbolTable.typeOf(currentToken.getValue()) + "." + currentToken.getNextToken().getNextToken().getValue();
            }else {
                name = name + "." + currentToken.getNextToken().getNextToken().getValue();
            }
            bw.write(process("identifier", currentToken.getValue())+"\n");
            bw.write(process("symbol", ".")+"\n");
            bw.write(process("identifier", currentToken.getValue())+"\n");
            bw.write(process("symbol", "(")+"\n");

            compileExpressionList(symbolTable,argsNum, className);

            bw.write(process("symbol", ")")+"\n");
            vmWriter.writeCall(name,String.valueOf(argsNum));

        }

        vmWriter.writePop("temp","0");

        bw.write(process("symbol", ";")+"\n");

        bw.write("</doStatement>"+"\n");

    }

    private void compileLet(SymbolTable symbolTable, String className) throws IOException {
        // 'let' varName ('['expression']')? '=' expression ';'
        bw.write("<letStatement>"+"\n");
        bw.write(process("keyword", "let")+"\n");
        String name = currentToken.getValue();
        bw.write(process("identifier", currentToken.getValue())+"\n");
        boolean isArray = false;
        if(currentToken.getValue().equals("[")){
            isArray = true;
            vmWriter.writePush(symbolTable.kindOf(name), symbolTable.indexOf(name));
            bw.write(process("symbol", "[")+"\n");
            compileExpression(symbolTable, className);
            bw.write(process("symbol", "]")+"\n");
            vmWriter.writeArithmetic("+");
        }
        bw.write(process("symbol", "=")+"\n");
        compileExpression(symbolTable, className);
        if(isArray){
            vmWriter.writePop("temp",String.valueOf(0));
            vmWriter.writePop("pointer",String.valueOf(1));
            vmWriter.writePush("temp",String.valueOf(0));
            vmWriter.writePop("that",String.valueOf(0));
        }else {
            vmWriter.writePop(symbolTable.kindOf(name),symbolTable.indexOf(name));
        }


        bw.write(process("symbol", ";")+"\n");

        bw.write("</letStatement>"+"\n");

    }
    private void compileParameterList(SymbolTable symbolTable, IntegerWrapper argsNum) throws IOException {
        // type varName (',' type varName)*

        String type = currentToken.getValue();
        bw.write(process("identifier,keyword", type)+"\n");
        String name = currentToken.getValue();
        bw.write(process("identifier", name)+"\n");

        symbolTable.define(name,type,"argument");

        while (currentToken.getValue().equals(",")) {
            increaseValue(argsNum);
            bw.write(process("symbol", ",")+"\n");
            type = currentToken.getValue();

            bw.write(process("identifier,keyword", type)+"\n");
            name = currentToken.getValue();

            bw.write(process("identifier", name)+"\n");
            symbolTable.define(name,type,"argument");

        }

    }
    public void compileClassVarDec(SymbolTable symbolTable) throws IOException {
        // Example: field int x;
        // 'static'|'field' type varName (',',varName)* ';'
        // type: 'int'|'char'|'boolean'|className
        // varName: identifier

        bw.write("<classVarDec>"+"\n");
        String tokenTypeValue = currentToken.getValue(); // static/field
        bw.write(process("keyword", "static,field")+"\n");
        String type = currentToken.getValue();
        bw.write(process("identifier,keyword", currentToken.getValue())+"\n");
        // code gen:
        symbolTable.define(currentToken.getValue(), type, tokenTypeValue);
        bw.write(process("identifier", currentToken.getValue())+"\n");

        while (currentToken.getValue().equals(",")) {
            bw.write(process("symbol", ",")+"\n");
            symbolTable.define(currentToken.getValue(), type, tokenTypeValue);
            bw.write(process("identifier", currentToken.getValue())+"\n");
        }
        bw.write(process("symbol", ";")+"\n");
        bw.write("</classVarDec>"+"\n");

    }
    public void compileVarDec(SymbolTable symbolTable) throws IOException {
        // 'var' type varName (',',varName)* ';'
        bw.write("<varDec>"+"\n");
        bw.write(process("keyword", "var")+"\n");
        String type = currentToken.getValue();
        bw.write(process("identifier,keyword", type)+"\n");
        String name = currentToken.getValue();
        bw.write(process("identifier", name)+"\n");

        symbolTable.define(name,type,"local");

        while (currentToken.getValue().equals(",")) {
            bw.write(process("symbol", ",")+"\n");
            name = currentToken.getValue();
            bw.write(process("identifier", name)+"\n");
            symbolTable.define(name,type,"local");

        }
        bw.write(process("symbol", ";")+"\n");
        bw.write("</varDec>"+"\n");

    }

    public static boolean isOperator(String str) {
        return str.equals("+") || str.equals("-") || str.equals("*") || str.equals("/")
                || str.equals("&amp;") || str.equals("|") || str.equals("&lt;") || str.equals("&gt;")
                || str.equals("=");
    }
    public static boolean isKeywordConstant(String str) {
        return str.equals("true") || str.equals("false") || str.equals("null") || str.equals("this");
    }

    public static String keywordConstantMap(String str) {
        return switch (str) {
            case "true" -> String.valueOf(-1);
            case "false" -> String.valueOf(0);
            case "null" -> String.valueOf(0);
            default -> throw new RuntimeException("不存在这个关键字常量，但是没写this的逻辑因为this是变量了！");
        };
    }

    public static boolean isUnaryOp(String str) {
        return str.equals("-") || str.equals("~");
    }


    private String process(String label, String variable) {
        // 1. 判断是否token的值（标签）应该为此，不是的话，要报错
        // 可以用逗号隔开，如static,field
        String type = currentToken.getType().toString();
        if (variable.contains(",") && !type.equals("stringConstant")) {
            String[] varList = variable.split(",");
            for (int i = 0; i < varList.length; i++) {
                if (varList[i].equals(currentToken.getValue())) {
                    break;
                }
                if (i == varList.length - 1) {
                    throw new RuntimeException("(value)缺少以下之一的值:" + Arrays.toString(varList) + "请检查源码");
                }

            }
        } else {
            if (!variable.equals(currentToken.getValue())) {
                throw new RuntimeException("(value)缺少值:" + variable + "请检查源码");
            }
        }

        // 符号可以用逗号隔开，如keyword,identifier
        if (label.contains(",")) {
            String[] labelList = label.split(",");
            for(int i = 0; i< labelList.length; i++){
                if (labelList[i].equals(currentToken.getType().toString())) {
                    break;
                }
                if(i == labelList.length -1){
                    throw new RuntimeException("(label)缺少以下之一的符号:" + Arrays.toString(labelList) + "请检查源码");

                }
            }
        } else {
            if (!label.equals(currentToken.getType().toString())) {
                throw new RuntimeException("(label)缺少符号:" + label + "请检查源码");
            }
        }


        // 2. 满足则替换,写入xml
        String demo = "  <label>variable</label>";
        demo = demo.replace("label", currentToken.getType().toString()).replace("variable", currentToken.getValue());

        // 3. 更新token
        if (currentToken.hasMoreTokens()) {
            currentToken = currentToken.getNextToken();
        }

        return demo;
    }

    public static void increaseValue(IntegerWrapper wrapper) {
        wrapper.value = wrapper.value + 1;
    }
}
