package com.jack.analyzer;

import com.jack.analyzer.data.Token;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {

        // 1. 程序入口：java -jar jack.jar [filename]/[directory]
        if (args.length != 1) {
            throw new RuntimeException("You must Enter file or directory and not more than 1 argument");
        }
        String input = args[0];
//        bw.write("参数" + 1 + "的值为：" + input);
        String output;

        // 2. 文件处理：如果单一文件直接加入列表，如果多文件，将多文件逐个加入列表
        List<File> handleFile = new ArrayList<>();
        if(input.contains(".jack")){
            File fileIn = new File(input);
            handleFile.add(fileIn);
        }else {
            File fileIn = new File(input);
            String[] filenames = fileIn.list(new MyExtFilter(".jack"));
            for(String name:filenames){
                handleFile.add(new File(input+ "/" + name));
            }
        }



//        bw.write(handleFile);
//        bw.write(output);

        // 3. 编译过程（前端）：逐个文件编译
        for(File file:handleFile){

            output = file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(".")) ;

            File fileOut = new File(output+ "T.xml");
            File fileOutAnalyzer = new File(output+ ".xml");
            File vmOut = new File(output+ ".vm");
            // Step1:Tokenizer
            Tokenizer tokenizer = new Tokenizer(file,fileOut);
            tokenizer.tokenizeProcess();

            // Step2:Parser
            CompileEngine compileEngine = new CompileEngine(tokenizer.getFirstToken(),fileOutAnalyzer,vmOut);
            compileEngine.analyze();

        }

    }

    static class MyExtFilter implements FilenameFilter {

        private String ext;

        MyExtFilter(String ext) {
            this.ext = ext;
        }

        public boolean accept(File dir, String name) {
            return name.endsWith(ext);
        }

    }
}