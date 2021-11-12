package compiler;

import compiler.diagnostics.*;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCompilerMessage {

    @Test
    public void compilerMessageTest1() {
        String s = """
                ) -> Option<String> {
                    for ann in annotations {
                        match (ann.range.0, ann.range.1) {
                            (None, None) => continue,
                            (Some(start), Some(end)) if start > end_index => continue,
                            (Some(start), Some(end)) if start >= start_index => {
                                let label = if let Some(ref label) = ann.label {
                                    format!(" {}", label)
                                } else {
                                    String::from("")
                                };
                                return Some(format!(
                                    "{}{}{}",
                                    " ".repeat(start - start_index),
                                    "^".repeat(end - start),
                                    label
                                ));
                            }
                            _ => continue,
                        }
                    }
                """;

        var msg = new CompilerError() {
            @Override
            public void generate(Source source) {
                this.setMessage("Something went badly");
                this.addPrimaryAnnotation(new Span(106, 331));
                this.addSecondaryAnnotation(new Span(5, 14), "C");
                this.addSecondaryAnnotation(new Span(0, 8), "B");
                this.addPrimaryAnnotation(Span.fromStartEnd(0, 418), "Expected B");
                this.addSecondaryAnnotation(Span.fromStartEnd(421, 427), "Blocking a label");
                this.addSecondaryAnnotation(new Span(46, 601), "hey");

                this.addPrimaryAnnotation(new Span(221, 5), "Whats defined right here...");
                this.addPrimaryAnnotation(new Span(243, 5), "... is used right here!");
                this.addSecondaryAnnotation(new Span(215, 50), "The entire line");
            }
        };

        var expectedResult = """
                ERROR: Something went badly
                   |\s
                 1 |     ) -> Option<String> {
                   |  ___^    -------------- C
                   | |   -------- B
                 2 | |       for ann in annotations {
                   | | __________________________-
                 3 | ||          match (ann.range.0, ann.range.1) {
                 4 | ||              (None, None) => continue,
                   | || _____________^
                 5 | |||             (Some(start), Some(end)) if start > end_index => continue,
                 6 | |||             (Some(start), Some(end)) if start >= start_index => {
                   | |||             -------------------------------------------------- The entire line
                   | |||                   ^^^^^                 ^^^^^ ... is used right here!
                   | |||                   Whats defined right here...
                 7 | |||                 let label = if let Some(ref label) = ann.label {
                 8 | |||                     format!(" {}", label)
                 9 | |||                 } else {
                10 | |||                     String::from("")
                   | |||                 ^   ------         ^
                   | |||                 |   Blocking a label
                   | |||_________________| Expected B       |
                   |  ||____________________________________|
                11 |  |                  };
                12 |  |                  return Some(format!(
                ...   | \s
                15 |  |                      "^".repeat(end - start),
                16 |  |                      label
                   |  |__________________________- hey
                            
                1 error and 0 warnings occurred while compiling.
                """;

        var writer = new StringWriter();

        var reporter = new CompilerMessageReporter(new PrintWriter(writer), CompilerMessageReporter.Colors.OFF, s);
        reporter.reportMessage(msg);
        reporter.finish();

        assertEquals(expectedResult, writer.toString());
    }

    @Test
    public void compilerMessageTest2() {
        String s = """
                fn main() {
                    let x = if true {
                        1i32
                    } else {
                        1u32 + 4u32
                    };
                }
                """;

        var msg = new CompilerError() {
            @Override
            public void generate(Source source) {
                this.setMessage("`if` and `else` have incompatible types");
                this.addPrimaryAnnotation(Span.fromStartEnd(68, 79), "expected `i32`, found `u32`");
                this.addSecondaryAnnotation(Span.fromStartEnd(42, 46), "expected because of this");
                this.addSecondaryAnnotation(Span.fromStartEnd(24, 85), "`if` and `else`·have incompatible types");
                this.addNote("For more information about this error,\ntry `rustc --explain E0308`.");
            }
        };

        var expectedResult = """
                ERROR: `if` and `else` have incompatible types
                  |\s
                2 |   let x = if true {
                  |  _________-
                3 | |     1i32
                  | |     ---- expected because of this
                4 | | } else {
                5 | |     1u32 + 4u32
                  | |     ^^^^^^^^^^^ expected `i32`, found `u32`
                6 | | };
                  | |_- `if` and `else`·have incompatible types
                  = note: For more information about this error,
                          try `rustc --explain E0308`.

                1 error and 0 warnings occurred while compiling.
                """;

        var writer = new StringWriter();

        var reporter = new CompilerMessageReporter(new PrintWriter(writer), CompilerMessageReporter.Colors.OFF, s);
        reporter.reportMessage(msg);
        reporter.finish();

        assertEquals(expectedResult, writer.toString());
    }

    @Test
    public void compilerMessageTest3() {
        String s = """
                class Point {
                    int x;
                    int y;
                    
                    public Point getManhattenDistance() {
                        return x + y;
                    }
                }
                                
                class Main {
                    public static void main(String[] args) {
                    
                    }
                }
                """;

        var msg = new CompilerError() {
            @Override
            public void generate(Source source) {
                this.setMessage("Return of unexpected type.");
                this.addPrimaryAnnotation(Span.fromStartEnd(94, 99), "returning value of type `int`");
                this.addSecondaryAnnotation(Span.fromStartEnd(48, 53), "expected type `Point`");
                this.addSecondaryAnnotation(Span.fromStartEnd(41, 106), "error in this method");
            }
        };

        var expectedResult = """
                ERROR: Return of unexpected type.
                  |\s
                5 |   public Point getManhattenDistance() {
                  |  _-      ----- expected type `Point`
                6 | |     return x + y;
                  | |            ^^^^^ returning value of type `int`
                7 | | }
                  | |_- error in this method

                1 error and 0 warnings occurred while compiling.
                """;

        var writer = new StringWriter();

        var reporter = new CompilerMessageReporter(new PrintWriter(writer), CompilerMessageReporter.Colors.OFF, s);
        reporter.reportMessage(msg);
        reporter.finish();

        assertEquals(expectedResult, writer.toString());
    }
}
