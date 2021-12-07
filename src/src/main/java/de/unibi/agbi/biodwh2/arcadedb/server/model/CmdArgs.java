package de.unibi.agbi.biodwh2.arcadedb.server.model;

import picocli.CommandLine;

@CommandLine.Command(name = "BioDWH2-ArcadeDB-Server.jar", sortOptions = false, separator = " ", footer = "Visit https://biodwh2.github.io for more documentation.")
public class CmdArgs {
    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "print this message", order = 1)
    public boolean help;
    @CommandLine.Option(names = {
            "-s", "--start"
    }, arity = "1", paramLabel = "<workspacePath>", description = "Start an ArcadeDB server for the workspace", order = 2)
    public String start;
    @CommandLine.Option(names = {
            "-c", "--create"
    }, arity = "1", paramLabel = "<workspacePath>", description = "Create an ArcadeDB database from the workspace graph", order = 3)
    public String create;
    @CommandLine.Option(names = {
            "-cs", "--create-start"
    }, arity = "1", paramLabel = "<workspacePath>", description = "Create and start an ArcadeDB database from the workspace graph", order = 4)
    public String createStart;
    @CommandLine.Option(names = {
            "-p", "--port"
    }, defaultValue = "2480-2489", paramLabel = "<port>", description = "Specifies the ArcadeDB server port(-range) (default 2480-2489)", order = 5)
    public String port;
}
