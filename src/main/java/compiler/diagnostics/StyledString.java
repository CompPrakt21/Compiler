package compiler.diagnostics;

import java.util.ArrayList;
import java.util.List;

public class StyledString {
    record Element(String text, TextStyle style) {
    }

    List<Element> content;

    StyledString() {
        this.content = new ArrayList<>();
    }

    void add(String text, TextStyle style) {
        this.content.add(new Element(text, style));
    }
}
