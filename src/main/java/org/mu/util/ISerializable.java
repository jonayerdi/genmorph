package org.mu.util;

import java.io.IOException;
import java.io.Writer;

public interface ISerializable {
    void writeTo(final Writer writer) throws IOException;
}
