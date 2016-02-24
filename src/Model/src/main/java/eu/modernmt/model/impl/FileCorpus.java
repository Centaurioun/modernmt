package eu.modernmt.model.impl;

import eu.modernmt.model.Corpus;

import java.io.*;
import java.util.Locale;

/**
 * Created by davide on 10/07/15.
 */
public class FileCorpus implements Corpus {

    private File file;
    private String name;
    private Locale language;

    private static String getNameFromFile(File file) {
        String fullname = file.getName();
        int lastDot = fullname.lastIndexOf('.');
        return fullname.substring(0, lastDot);
    }

    private static Locale getLangFromFile(File file) {
        String fullname = file.getName();
        int lastDot = fullname.lastIndexOf('.');
        return Locale.forLanguageTag(fullname.substring(lastDot + 1));
    }

    public FileCorpus(File file) {
        this(file, getNameFromFile(file));
    }

    public FileCorpus(File file, String name) {
        this(file, name, getLangFromFile(file));
    }

    public FileCorpus(File file, String name, Locale language) {
        this.file = file;
        this.name = name;
        this.language = language;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Locale getLanguage() {
        return language;
    }

    @Override
    public Reader getContentReader() throws FileNotFoundException {
        try {
            return new InputStreamReader(new FileInputStream(file), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Error("UTF-8 not supported");
        }
    }

    @Override
    public Writer getContentWriter(boolean append) throws IOException {
        return new FileWriter(file, append);
    }

    @Override
    public String toString() {
        return name + "." + language;
    }

}
