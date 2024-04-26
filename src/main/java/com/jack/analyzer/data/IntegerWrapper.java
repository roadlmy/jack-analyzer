package com.jack.analyzer.data;

public class IntegerWrapper {
    public Integer value;
    
    public IntegerWrapper(Integer value) {
        this.value = value;
    }

    // 重写toString方法以返回value的字符串表示
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}