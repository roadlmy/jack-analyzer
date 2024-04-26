package com.jack.analyzer;

import com.jack.analyzer.data.Token;
import com.jack.analyzer.enumeration.TokenTypeEnum;

import java.io.*;

public class Tokenizer {

    File file;
    PushbackReader pushbackReader;
    Token token;
    Token firstToken;
    File fileOut;

    BufferedWriter bw;


    public Tokenizer(File file,File fileOut) throws IOException {
        this.file = file;
        this.fileOut = fileOut;
        this.pushbackReader = new PushbackReader(new FileReader(file));
        this.bw = new BufferedWriter(new FileWriter(fileOut));

        this.token = new Token();
        this.firstToken = token;
    }

    public void tokenizeProcess() throws IOException {
        bw.write("<tokens>\n");
        int data;
        while ((data = pushbackReader.read()) != -1) {
            // 处理jack文件
            // 1. 处理空格和换行：忽略
            char charData = (char) data;
            if(charData == '\n' || charData == '\s'){
                continue;
            }

            // 2. 处理注释 包含: 2.1 //.... 2.2 /** ... */ 2.3 /** .. *.. *.. *.. */ 策略：忽略
            if(charData == '/' && (peek() == '/' || peek() == '*')){
                handleComment();
                continue;
            }

            // 3. 处理keyword和identifier
            if(isCharacterInRange(charData)){
                handleKeywordOrIdentifier(charData);
                continue;
            }

            // 4.处理stringConstant
            if(charData == '"'){
                handleStringConstant();
                continue;
            }

            // 5.处理intConstant
            if(isDigit(charData)){
                handleIntConstant(charData);
                continue;
            }

            // 6. 处理symbol
            if(isSymbol(charData)){
                handleSymbol(charData);
                continue;
            }
//            System.out.print((char) data);
        }
        pushbackReader.close();
        bw.write("</tokens>");
        bw.flush();
        bw.close();

    }

    private void handleIntConstant(char charData1) throws IOException {
        int data;
        StringBuilder bufferBuilder = new StringBuilder();
        bufferBuilder.append(charData1);
        while ((data = pushbackReader.read()) != -1) {
            char charData = (char) data;
            if (isDigit(charData)) {
                bufferBuilder.append(charData);
            } else {
                // 当不是数字时，表示常数结束
                pushbackReader.unread(charData); // 将非数字字符放回流中
                Token newToken = new Token(TokenTypeEnum.INT_CONSTANT, bufferBuilder.toString(), null);
                this.bw.write("<integerConstant>" + bufferBuilder + "</integerConstant>\n");
                this.token.setNextToken(newToken);
                this.token = newToken;
                return;
            }
        }

        // 如果没有找到整数常数的结尾，可能是代码中的错误，你可以在这里抛出异常或者打印错误信息
        throw new IOException("Unterminated integer constant: " + bufferBuilder.toString());
    }

    private void handleStringConstant() throws IOException {
        int data;
        StringBuilder bufferBuilder = new StringBuilder();
        while ((data = pushbackReader.read()) != -1) {
            char charData1 = (char) data;


            if (charData1 == '"') {
                // 到达字符串结尾
                Token newToken = new Token(TokenTypeEnum.STRING_CONSTANT, bufferBuilder.toString(), null);
                this.bw.write("<stringConstant>" + bufferBuilder + "</stringConstant>\n");

                this.token.setNextToken(newToken);
                this.token = newToken;
                return;
            }

            bufferBuilder.append(charData1);
        }

        // 如果没有找到字符串的结尾，可能是代码中的错误，你可以在这里抛出异常或者打印错误信息
        throw new IOException("Unterminated string constant: " + bufferBuilder.toString());
    }

    private void handleKeywordOrIdentifier(char charData) throws IOException {
        int data;
        StringBuilder bufferBuilder = new StringBuilder();
        bufferBuilder.append(charData);
        while ((data = pushbackReader.read()) != -1) {
            char charData1 = (char) data;
            if(!isCharacterValid(charData1)){
                // 到结尾
                pushbackReader.unread(data);
                Token newToken = null;
                if(TokenTypeEnum.isKeyword(bufferBuilder.toString())){
                    newToken = new Token(TokenTypeEnum.KEYWORD, bufferBuilder.toString(), null);
                    this.bw.write("<keyword>" +bufferBuilder +"</keyword>\n");

                }else {
                    newToken = new Token(TokenTypeEnum.IDENTIFIER, bufferBuilder.toString(), null);
                    this.bw.write("<identifier>" +bufferBuilder +"</identifier>\n");


                }

                this.token.setNextToken(newToken);
                this.token = newToken;

                return;

            }else {
                bufferBuilder.append(charData1);
            }
        }

        // 如果没有找到关键字或标识符的结尾，可能是代码中的错误，你可以在这里抛出异常或者打印错误信息
        throw new IOException("Unterminated keyword or identifier: " + bufferBuilder.toString());


    }

    private char peek() throws IOException {
        char read = (char)pushbackReader.read();

        pushbackReader.unread(read);

        return read;

    }

    private void handleComment() throws IOException {
        int data;
        char nextChar = peek();
        if (nextChar == '/') {
            // 单行注释
            while ((data = pushbackReader.read()) != -1 && (char) data != '\n');
        } else if (nextChar == '*') {
            // 多行注释
            StringBuilder bufferBuilder = new StringBuilder();
            bufferBuilder.append("/");
            while ((data = pushbackReader.read()) != -1) {
                char charData = (char) data;
                bufferBuilder.append(charData);
                if (charData == '*' && peek() == '/') {
                    bufferBuilder.append('/');
                    pushbackReader.read(); // 读取注释结束符 '/'
                    return;
                }
            }
            // 如果没有找到注释的结尾，可能是代码中的错误，你可以在这里抛出异常或者打印错误信息
            throw new IOException("Unterminated comment: " + bufferBuilder.toString());
        } else {
            // 不是注释，回退字符并结束方法
            pushbackReader.unread(nextChar);
        }
    }

    public static boolean isCharacterInRange(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    public static boolean isCharacterValid(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private boolean isDigit(char c) {
        return Character.isDigit(c);
    }

    private boolean isSymbol(char c) throws IOException {
        // 在注释中的情况下，即使是符号也不需要处理
        if (c == '/') {
            char nextChar = peek();
            if (nextChar == '/' || nextChar == '*') {
                return false;
            }
        }
        // 在这里列出您需要处理的符号，比如+、-、*、/、=等
        // 这里只是一个示例，您需要根据您的需求添加更多符号
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '=' || c == ';' || c == '(' || c == ')' || c == '[' || c == ']' || c == '{' || c == '}' || c == ',' || c == '.' || c == '&' || c == '|' || c == '<' || c == '>' || c == '~';
    }

    private void handleSymbol(char charData) throws IOException {
        String symbol = String.valueOf(charData);
        // 进行转义处理
        switch (charData) {
            case '<':
                symbol = "&lt;";
                break;
            case '>':
                symbol = "&gt;";
                break;
            case '&':
                symbol = "&amp;";
                break;
            // 可以根据需要添加其他特殊字符的转义处理
        }
        // 处理符号逻辑
        Token newToken = new Token(TokenTypeEnum.SYMBOL, symbol, null);
        this.bw.write("<symbol>" + symbol + "</symbol>\n");

        this.token.setNextToken(newToken);
        this.token = newToken;
    }


    public Token getFirstToken() {
        return firstToken;
    }
}
