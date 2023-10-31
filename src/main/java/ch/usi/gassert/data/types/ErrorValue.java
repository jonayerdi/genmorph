package ch.usi.gassert.data.types;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ErrorValue {

    public static final Pattern pattern = Pattern.compile("^ERROR\\[(\\w*)\\]$");

    public final String type;

    public ErrorValue(final String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "ERROR[" + type + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ErrorValue) {
            return this.type.equals(((ErrorValue)obj).type);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type);
    }

    public static ErrorValue parse(final String text) {
        final Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return new ErrorValue(matcher.group(1));
        } else {
            return null;
        }
    }

}
