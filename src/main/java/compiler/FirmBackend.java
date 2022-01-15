package compiler;

import firm.Backend;
import firm.Util;

import java.io.File;
import java.io.IOException;

public class FirmBackend {

    private FrontendResult frontend;
    private File asmOutputFile;
    private File runtimeFile;

    public FirmBackend(File asmOutput, File runtimeFile, FrontendResult frontend) {
        this.frontend = frontend;
        this.asmOutputFile = asmOutput;
        this.runtimeFile = runtimeFile;
    }

    public void generateASM(boolean dumpGraphs) {

        var translation = new Translation(this.frontend);
        translation.translate(dumpGraphs);

        Util.lowerSels();

        try {
            var execFilename = "a.out";

            Backend.createAssembler(asmOutputFile.getAbsolutePath(), this.frontend.inputFile().getName());

            ProcessBuilder pb = new ProcessBuilder("gcc", "-o", execFilename, this.asmOutputFile.getAbsolutePath(), this.runtimeFile.getAbsolutePath());
            pb.inheritIO();
            pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
