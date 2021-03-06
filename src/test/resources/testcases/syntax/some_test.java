/* public classes dont exist */
public class DerCompiler {
    
        public static void main(String[] args){
            CommandLineBuilder clb = new CommandLineBuilder();
            clb.parseArguments(args);
    
            CommandLineOptions options = clb.parseArguments(args);
    
            CompilerSetup.setupGlobalValues(options);
            Action action = new CompilerSetup().parseAction(options);
    
            boolean showHelp = options.help();
    
            options.finish();
    
            if (showHelp) {
                action.help();
            } else {
                action.run();
            }
            System.exit(0);
        }
    }
