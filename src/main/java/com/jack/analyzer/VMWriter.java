package com.jack.analyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class VMWriter {

    BufferedWriter vmWriter;

    public VMWriter(File vmOut) throws IOException {
        this.vmWriter = new BufferedWriter(new FileWriter(vmOut));
    }

    public void writePush(String segment, String index) throws IOException {
        vmWriter.write("push "+ segment + " " + index + "\n");
    }

    public void writePop(String segment, String index) throws IOException {
        vmWriter.write("pop "+ segment + " " + index + "\n");
    }

    public void writeArithmetic(String command) throws IOException {
        if (command.equals("+")){
            vmWriter.write("add\n");
        }else if (command.equals("-")){
            vmWriter.write("sub\n");
        }else if (command.equals("=")){
            vmWriter.write("eq\n");
        }else if (command.equals("&gt;")){
            vmWriter.write("gt\n");
        }else if (command.equals("&lt;")){
            vmWriter.write("lt\n");
        }else if (command.equals("&amp;")){
            vmWriter.write("and\n");
        }else if (command.equals("|")){
            vmWriter.write("or\n");
        }else if (command.equals("*")){
            vmWriter.write("call Math.multiply 2\n");
        }else if (command.equals("/")){
            vmWriter.write("call Math.divide 2\n");
        }
        else {
            vmWriter.write(command + "\n");
        }
    }

    public void writeUnaryArithmetic(String command) throws IOException {
        if(command.equals("-")){
            vmWriter.write("neg\n");
        }else if (command.equals("~")){
            vmWriter.write("not\n");
        }
    }

    public void writeLabel(String label) throws IOException {
        vmWriter.write("label " + label + "\n");
    }

    public void writeGoto(String label) throws IOException {
        vmWriter.write("goto" + " " +label + "\n");
    }

    public void writeIf(String label) throws IOException {
        vmWriter.write("if-goto" + " " +label + "\n");
    }

    public void writeCall(String name, String nArgs) throws IOException {
        vmWriter.write("call "+ name + " " + nArgs + "\n");
    }

    public void writeFunction(String name, String nLocals) throws IOException {
        vmWriter.write("function "+ name + " " + nLocals + "\n");
    }

    public void close() throws IOException {
        this.vmWriter.flush();
        this.vmWriter.close();
    }

    public void writeReturn() throws IOException {
        vmWriter.write("return\n");

    }
}
