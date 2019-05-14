package eu.modernmt.processing.tokenizer.jflex;

import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Sentence;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.TokenizedString;

import java.io.IOException;
import java.io.Reader;

/**
 * Created by davide on 29/01/16.
 */
public abstract class JFlexTokenAnnotator implements BaseTokenizer.Annotator {

    protected static final int YYEOF = -1;

    protected static int word(int leftOffset, int rightOffset) {
        return protect(leftOffset, rightOffset, true);
    }

    protected static int protect(int leftOffset, int rightOffset) {
        return protect(leftOffset, rightOffset, false);
    }

    protected static int protect(int leftOffset, int rightOffset, boolean truncate) {
        return ((truncate ? 0x01 : 0x00) << 16) +
                ((leftOffset & 0xFF) << 8) +
                (rightOffset & 0xFF);
    }

    @Override
    public final void annotate(TokenizedString text) throws ProcessingException {
        this.yyreset(text.getReader());

        try {
            int type;
            while ((type = next()) != JFlexTokenAnnotator.YYEOF) {
                this.annotate(text, type);
            }
        } catch (IOException e) {
            throw new ProcessingException(e);
        }
    }

    protected final void annotate(TokenizedString text, int tokenType) {
        boolean truncate = ((tokenType >> 16) & 0xFF) > 0;
        int leftOffset = (tokenType >> 8) & 0xFF;
        int rightOffset = tokenType & 0xFF;

        int begin = yychar();
        int end = begin + getMarkedPosition() - getStartRead();

        int lastIndex;
        if (truncate)
            lastIndex = text.setWord(begin + leftOffset, end - rightOffset);
        else
            lastIndex = text.protect(begin + leftOffset, end - rightOffset);

        if (lastIndex != -1)
            this.yypushback(end - lastIndex - 1);  // set cursor right after last protected char

//        switch (tokenType) {
//            case PROTECT:
//                text.protect(begin + 1, end);
//                break;
//            case PROTECT_ALL:
//                text.protect(begin, end);
//                break;
//            case PROTECT_RIGHT:
//                text.protect(end);
//                break;
//            case WORD:
//                text.setWord(begin, end);
//                break;
//        }
    }

    public abstract void yyreset(Reader reader);

    public abstract int next() throws IOException;

    protected abstract int getStartRead();

    protected abstract int getMarkedPosition();

    protected abstract int yychar();

    protected abstract void yypushback(int number);

    public static void main(String[] args) throws Throwable {
        String text;
        text = "Nuova pubblicazione|||Altre pubblicazioni...|Da pubblicazione esistente...|Da pubblicazione esistente...||Guida in linea Microsoft Publisher";

        Preprocessor preprocessor = new Preprocessor();
        LanguagePair language = new LanguagePair(Language.ITALIAN, Language.ITALIAN);

        try {
            Sentence sentence = preprocessor.process(language, text);
            System.out.println(TokensOutputStream.serialize(sentence, false, true));
        } finally {
            preprocessor.close();
        }
    }

}
