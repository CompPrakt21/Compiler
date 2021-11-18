package compiler;

import firm.Firm;

public class FirmTest {

    public static void printVersion() {
        // TODO: Remove this whole file once firm is integrated
        Firm.init();
        System.out.printf("Firm Version: %1s.%2s\n",
                Firm.getMajorVersion(), Firm.getMinorVersion());

    }
}
