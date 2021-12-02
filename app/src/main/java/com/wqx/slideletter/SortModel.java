package com.wqx.slideletter;

public class SortModel {

    private String name;
    private String letters;//显示拼音的首字母

    public boolean isNormalLanguage() {
        return isNormalLanguage;
    }

    public void setNormalLanguage(boolean normalLanguage) {
        isNormalLanguage = normalLanguage;
    }

    private boolean isNormalLanguage = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLetters() {
        return letters;
    }

    public void setLetters(String letters) {
        this.letters = letters;
    }
}
