![Java CI](https://github.com/BioDWH2/BioDWH2-ArcadeDB-Server/workflows/Java%20CI/badge.svg?branch=develop) ![Release](https://img.shields.io/github/v/release/BioDWH2/BioDWH2-ArcadeDB-Server) ![Downloads](https://img.shields.io/github/downloads/BioDWH2/BioDWH2-ArcadeDB-Server/total) ![License](https://img.shields.io/github/license/BioDWH2/BioDWH2-ArcadeDB-Server)

# BioDWH2-ArcadeDB-Server
**BioDWH2** is an easy-to-use, automated, graph-based data warehouse and mapping tool for bioinformatics and medical informatics. The main repository can be found [here](https://github.com/BioDWH2/BioDWH2).

This repository contains the **BioDWH2-ArcadeDB-Server** utility which can be used to create and explore an ArcadeDB graph database from any BioDWH2 workspace. There is no need for any ArcadeDB installation. All necessary components are bundled with this tool.

## Download
The latest release version of **BioDWH2-ArcadeDB-Server** can be downloaded [here](https://github.com/BioDWH2/BioDWH2-ArcadeDB-Server/releases/latest).

## Usage
BioDWH2-ArcadeDB-Server requires the Java Runtime Environment 11+. The JRE 11 archive is available [here](https://www.oracle.com/java/technologies/javase/jdk11-archive-downloads.html).

Creating a database from any workspace is done using the following command. Every time the workspace is updated or changed, the create command has to be executed again.
~~~BASH
> java -jar BioDWH2-ArcadeDB-Server.jar --create /path/to/workspace
~~~

Once the database has been created, the database and OrientDB Studio can be started as follows:
~~~BASH
> java -jar BioDWH2-ArcadeDB-Server.jar --start /path/to/workspace
~~~

## Help
~~~
Usage: BioDWH2-ArcadeDB-Server.jar [-h] [-c <workspacePath>] [-cs <workspacePath>]
                                   [-s <workspacePath>] [-p <port>] [-sp <studioPort>]
  -h, --help                            print this message
  -s, --start <workspacePath>           Start an ArcadeDB server for the workspace
  -c, --create <workspacePath>          Create an ArcadeDB database from the workspace graph
  -cs, --create-start <workspacePath>   Create and start an ArcadeDB database from the workspace graph
  -p, --port <port>                     Specifies the ArcadeDB server port(-range) (default 2480-2489)
~~~