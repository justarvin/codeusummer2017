package codeu.chat.util;

import java.io.IOException;

/**
 * Helper class to tokenize command line input.
 */
public final class Tokenizer {

    private StringBuilder token;
    private String source;
    private int at;

    public Tokenizer(String source) {
        this.source = source;
        this.at = 0;
        this.token = new StringBuilder();
    }

    public String next() throws IOException {
        //skip all leading whitespace
        while (remaining() > 0 && Character.isWhitespace(peek())) {
            read(); //ignore the result because we already know that it is a whitespace character
        }
        if (remaining() <= 0) {
            return null;
        } else if (peek() == '"') {
            return readWithQuotes();
        } else {
            return readWithNoQuotes();
        }
    }

    private int remaining() {
        return source.length() - at;
    }

    private char peek() throws IOException {
        if (at < source.length()) {
            return source.charAt(at);
        } else {
            throw new IOException("No character at given index.");
        }
    }

    private char read() throws IOException {
        final char c = peek();
        at += 1;
        return c;
    }

    private String readWithNoQuotes() throws IOException {
        token.setLength(0); //clear the token
        while (remaining() > 0 && !Character.isWhitespace(peek())) {
            token.append(read());
        }
        return token.toString();
    }

    private String readWithQuotes() throws IOException {
        token.setLength(0); //clear the token
        if (read() != '"') {
            throw new IOException("Strings must start with opening quote");
        }
        while (peek() != '"') {
            token.append(read());
        }
        read(); //read the closing quote that allows us to exit loop
        return token.toString();
    }
}
