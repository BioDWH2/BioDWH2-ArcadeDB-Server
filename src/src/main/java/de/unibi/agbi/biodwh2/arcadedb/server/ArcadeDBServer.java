package de.unibi.agbi.biodwh2.arcadedb.server;

import de.unibi.agbi.biodwh2.core.net.BioDWH2Updater;
import de.unibi.agbi.biodwh2.arcadedb.server.model.CmdArgs;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ArcadeDBServer {
    private static final Logger LOGGER = LogManager.getLogger(ArcadeDBServer.class);

    private ArcadeDBServer() {
    }

    public static void main(final String... args) {
        final CmdArgs commandLine = parseCommandLine(args);
        new ArcadeDBServer().run(commandLine);
    }

    private static CmdArgs parseCommandLine(final String... args) {
        final CmdArgs result = new CmdArgs();
        final CommandLine cmd = new CommandLine(result);
        cmd.parseArgs(args);
        return result;
    }

    private void run(final CmdArgs commandLine) {
        BioDWH2Updater.checkForUpdate("BioDWH2-ArcadeDB-Server",
                                      "https://api.github.com/repos/BioDWH2/BioDWH2-ArcadeDB-Server/releases");
        if (commandLine.createStart != null)
            createAndStartWorkspaceServer(commandLine);
        else if (commandLine.start != null)
            startWorkspaceServer(commandLine);
        else if (commandLine.create != null)
            createWorkspaceDatabase(commandLine);
        else
            printHelp(commandLine);
    }

    private void createAndStartWorkspaceServer(final CmdArgs commandLine) {
        final String workspacePath = commandLine.createStart;
        if (!verifyWorkspaceExists(workspacePath)) {
            printHelp(commandLine);
            return;
        }
        final ArcadeDBService service = new ArcadeDBService(workspacePath);
        service.deleteOldDatabase();
        service.startArcadeDBService(commandLine.port);
        service.createDatabase();
        storeWorkspaceHash(workspacePath);
        service.openBrowser();
    }

    private boolean verifyWorkspaceExists(final String workspacePath) {
        if (StringUtils.isEmpty(workspacePath) || !Paths.get(workspacePath).toFile().exists()) {
            if (LOGGER.isErrorEnabled())
                LOGGER.error("Workspace path '" + workspacePath + "' was not found");
            return false;
        }
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Using workspace directory '" + workspacePath + "'");
        return true;
    }

    private void printHelp(final CmdArgs commandLine) {
        CommandLine.usage(commandLine, System.out);
    }

    private void storeWorkspaceHash(final String workspacePath) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Updating workspace ArcadeDB cache checksum...");
        final Path hashFilePath = Paths.get(workspacePath, "arcadedb/checksum.txt");
        try {
            final String hash = HashUtils.getFastPseudoHashFromFile(
                    Paths.get(workspacePath, "sources/mapped.db").toString());
            final FileWriter writer = new FileWriter(hashFilePath.toFile());
            writer.write(hash);
            writer.close();
        } catch (IOException e) {
            if (LOGGER.isErrorEnabled())
                LOGGER.error("Failed to store hash of workspace mapped graph", e);
        }
    }

    private void startWorkspaceServer(final CmdArgs commandLine) {
        final String workspacePath = commandLine.start;
        if (!verifyWorkspaceExists(workspacePath)) {
            printHelp(commandLine);
            return;
        }
        if (!checkArcadeDBDatabaseMatchesWorkspace(workspacePath) && LOGGER.isInfoEnabled())
            LOGGER.warn("The ArcadeDB database is out-of-date and should be recreated with the --create command");
        final ArcadeDBService service = new ArcadeDBService(workspacePath);
        service.startArcadeDBService(commandLine.port);
        service.openBrowser();
    }

    private boolean checkArcadeDBDatabaseMatchesWorkspace(final String workspacePath) {
        try {
            final String hash = HashUtils.getFastPseudoHashFromFile(
                    Paths.get(workspacePath, "sources/mapped.db").toString());
            final Path hashFilePath = Paths.get(workspacePath, "arcadedb/checksum.txt");
            if (Files.exists(hashFilePath)) {
                final String storedHash = new String(Files.readAllBytes(hashFilePath)).trim();
                return hash.equals(storedHash);
            }
        } catch (IOException e) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Failed to check hash of workspace mapped graph", e);
        }
        return false;
    }

    private void createWorkspaceDatabase(final CmdArgs commandLine) {
        final String workspacePath = commandLine.create;
        if (!verifyWorkspaceExists(workspacePath)) {
            printHelp(commandLine);
            return;
        }
        final ArcadeDBService service = new ArcadeDBService(workspacePath);
        service.deleteOldDatabase();
        service.startArcadeDBService(commandLine.port);
        service.createDatabase();
        storeWorkspaceHash(workspacePath);
        service.stopArcadeDBService();
    }
}
