package eu.modernmt.processing.tokenizer;

import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.string.InvalidOperationException;
import eu.modernmt.processing.framework.string.XMLEditableString;

import java.util.List;

/**
 * Created by davide on 19/02/16.
 */
@Deprecated
public class TokenizerOutputTransformer {

    @Deprecated
    public static XMLEditableString transform(XMLEditableString text, String[] tokens) throws ProcessingException {
        XMLEditableString.Editor editor = text.getEditor();

        String string = text.toString();
        int length = string.length();

        int stringIndex = 0;

        for (String token : tokens) {
            int tokenPos = string.indexOf(token, stringIndex);

            if (tokenPos < 0)
                throw new ProcessingException("Unable to find token '" + token + "' starting from index " + stringIndex + " in sentence \"" + text + "\"");

            int tokenLength = token.length();

            stringIndex = tokenPos + tokenLength;
            if (stringIndex <= length)
                setWord(string, editor, tokenPos, tokenLength);
        }

        return editor.commitChanges();
    }

    private static void setWord(String string, XMLEditableString.Editor editor, int startIndex, int length) throws InvalidOperationException {
        int end = startIndex + length;
        boolean hasRightSpace = end < string.length() && string.charAt(end) == ' ';

        editor.setWord(startIndex, length, hasRightSpace);
    }

    @Deprecated
    public static XMLEditableString transform(XMLEditableString string, List<String> tokens) throws ProcessingException {
        return transform(string, tokens.toArray(new String[tokens.size()]));
    }

}
