package de.unibi.agbi.biodwh2.arcadedb.server;

import com.arcadedb.ContextConfiguration;
import com.arcadedb.GlobalConfiguration;
import com.arcadedb.database.DatabaseInternal;
import com.arcadedb.database.RID;
import com.arcadedb.graph.MutableEdge;
import com.arcadedb.graph.MutableVertex;
import com.arcadedb.graph.Vertex;
import com.arcadedb.index.lsm.LSMTreeIndexAbstract;
import com.arcadedb.schema.EdgeType;
import com.arcadedb.schema.Schema;
import com.arcadedb.schema.Type;
import com.arcadedb.schema.VertexType;
import com.arcadedb.server.ArcadeDBServer;
import de.unibi.agbi.biodwh2.core.model.graph.Edge;
import de.unibi.agbi.biodwh2.core.model.graph.Graph;
import de.unibi.agbi.biodwh2.core.model.graph.IndexDescription;
import de.unibi.agbi.biodwh2.core.model.graph.Node;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * https://docs.arcadedb.com/#Embed-Server
 */
public class ArcadeDBService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArcadeDBService.class);

    private final String workspacePath;
    private final Path databasePath;
    private ArcadeDBServer server;

    public ArcadeDBService(final String workspacePath) {
        this.workspacePath = workspacePath;
        databasePath = Paths.get(workspacePath, "arcadedb");
        InjectionLogger.inject();
    }

    public void startArcadeDBService(String port) {
        port = validatePort(port);
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Starting ArcadeDB DBMS on localhost:" + port + "...");
        server = new ArcadeDBServer(getServerConfig(port));
        server.start();
    }

    private String validatePort(String port) {
        final String fallback = "2480-2489";
        port = port.trim().replace(" ", "");
        if (port.contains("-")) {
            final String[] rangeParts = StringUtils.split(port, '-');
            if (rangeParts.length == 2) {
                return port;
            } else
                LOGGER.warn("Failed to parse port or port range '" + port + "', falling back to '" + fallback + "'");
        }
        try {
            final int portNumber = Integer.parseInt(port);
            return String.valueOf(Math.abs(portNumber));
        } catch (NumberFormatException ignored) {
            LOGGER.warn("Failed to parse port or port range '" + port + "', falling back to '" + fallback + "'");
        }
        return fallback;
    }

    private ContextConfiguration getServerConfig(final String port) {
        final ContextConfiguration config = new ContextConfiguration();
        config.setValue(GlobalConfiguration.HA_SERVER_LIST, "localhost");
        config.setValue(GlobalConfiguration.HA_REPLICATION_INCOMING_HOST, "0.0.0.0");
        config.setValue(GlobalConfiguration.HA_ENABLED, false);
        config.setValue(GlobalConfiguration.SERVER_NAME, "BioDWH2-ArcadeDB-Server");
        config.setValue(GlobalConfiguration.SERVER_HTTP_INCOMING_PORT, port);
        config.setValue(GlobalConfiguration.SERVER_ROOT_PASSWORD, "biodwh2-arcadedb");
        config.setValue(GlobalConfiguration.SERVER_ROOT_PATH, databasePath);
        return config;
    }

    public void stopArcadeDBService() {
        if (server != null)
            server.stop();
    }

    public void deleteOldDatabase() {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Removing old database...");

        try {
            FileUtils.deleteDirectory(databasePath.toFile());
        } catch (IOException e) {
            if (LOGGER.isErrorEnabled())
                LOGGER.error("Failed to remove old database '" + databasePath + "'", e);
        }
    }

    public void createDatabase() {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Creating ArcadeDB database...");
        final DatabaseInternal db = server.createDatabase("BioDWH2");
        db.getConfiguration().setValue(GlobalConfiguration.BUCKET_DEFAULT_PAGE_SIZE, 1048576);
        try (Graph graph = new Graph(Paths.get(workspacePath, "sources/mapped.db"), true)) {
            Files.createDirectories(databasePath);
            final HashMap<Long, RID> nodeIdArcadeDBIdMap = new HashMap<>();
            createNodes(db, graph, nodeIdArcadeDBIdMap);
            createEdges(db, graph, nodeIdArcadeDBIdMap);
            createIndices(db, graph);
        } catch (IOException e) {
            if (LOGGER.isErrorEnabled())
                LOGGER.error("Failed to create ArcadeDB database '" + databasePath + "'", e);
        }
    }

    private void createNodes(final DatabaseInternal db, final Graph graph,
                             final HashMap<Long, RID> nodeIdArcadeDBIdMap) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Creating node definitions...");
        for (final String label : graph.getNodeLabels()) {
            final VertexType definition = db.getSchema().createVertexType(label);
            final Map<String, de.unibi.agbi.biodwh2.core.lang.Type> propertyKeyTypes = graph.getPropertyKeyTypesForNodeLabel(
                    label);
            for (final String key : propertyKeyTypes.keySet())
                if (!Node.IGNORED_FIELDS.contains(key))
                    definition.createProperty(key, getTypeByPropertyType(propertyKeyTypes.get(key)));
        }
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Creating nodes...");
        for (final Node node : graph.getNodes()) {
            final String label = node.getLabel();
            final MutableVertex arcadeNode = db.newVertex(label).modify();
            for (final String propertyKey : node.keySet())
                setPropertySafe(node, arcadeNode, propertyKey);
            final RID id = arcadeNode.save().getIdentity();
            nodeIdArcadeDBIdMap.put(node.getId(), id);
        }
    }

    private Type getTypeByPropertyType(de.unibi.agbi.biodwh2.core.lang.Type propertyType) {
        Type type = Type.getTypeByClass(propertyType.getType());
        if (type == null && propertyType.isList()) {
            Class<?> componentType = propertyType.getComponentType();
            // Fallback to string if component type is unknown
            if (componentType == null)
                componentType = String.class;
            type = Type.getTypeByClass(Array.newInstance(componentType, 0).getClass());
        }
        return type;
    }

    private void setPropertySafe(final Node node, final MutableVertex arcadeNode, final String propertyKey) {
        try {
            if (!Node.IGNORED_FIELDS.contains(propertyKey)) {
                Object value = node.getProperty(propertyKey);
                if (value instanceof Collection)
                    value = convertCollectionToArray((Collection<?>) value);
                if (value != null)
                    arcadeNode.set(propertyKey, value);
            }
        } catch (Exception e) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn(
                        "Illegal property '" + propertyKey + " -> " + node.getProperty(propertyKey) + "' for node '" +
                        node.getId() + "[:" + node.getLabel() + "]'");
        }
    }

    @SuppressWarnings({"SuspiciousToArrayCall"})
    private Object convertCollectionToArray(final Collection<?> collection) {
        Class<?> type = null;
        for (Object t : collection) {
            if (t != null) {
                type = t.getClass();
                break;
            }
        }
        if (type != null) {
            if (type.equals(String.class))
                return collection.stream().map(type::cast).toArray(String[]::new);
            if (type.equals(Boolean.class))
                return collection.stream().map(type::cast).toArray(Boolean[]::new);
            if (type.equals(Integer.class))
                return collection.stream().map(type::cast).toArray(Integer[]::new);
            if (type.equals(Float.class))
                return collection.stream().map(type::cast).toArray(Float[]::new);
            if (type.equals(Long.class))
                return collection.stream().map(type::cast).toArray(Long[]::new);
            if (type.equals(Double.class))
                return collection.stream().map(type::cast).toArray(Double[]::new);
            if (type.equals(Byte.class))
                return collection.stream().map(type::cast).toArray(Byte[]::new);
            if (type.equals(Short.class))
                return collection.stream().map(type::cast).toArray(Short[]::new);
        }
        return collection.stream().map(Object::toString).toArray(String[]::new);
    }

    private void createEdges(final DatabaseInternal db, final Graph graph,
                             final HashMap<Long, RID> nodeIdArcadeDBIdMap) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Creating edge definitions...");
        for (final String label : graph.getEdgeLabels()) {
            final EdgeType definition = db.getSchema().createEdgeType(label);
            final Map<String, de.unibi.agbi.biodwh2.core.lang.Type> propertyKeyTypes = graph.getPropertyKeyTypesForEdgeLabel(
                    label);
            for (final String key : propertyKeyTypes.keySet())
                if (!Edge.IGNORED_FIELDS.contains(key))
                    definition.createProperty(key, getTypeByPropertyType(propertyKeyTypes.get(key)));
        }
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Creating edges...");
        for (final Edge edge : graph.getEdges()) {
            final Vertex fromNode = db.lookupByRID(nodeIdArcadeDBIdMap.get(edge.getFromId()), false).asVertex(false);
            final RID toNodeId = nodeIdArcadeDBIdMap.get(edge.getToId());
            final MutableEdge arcadeEdge = fromNode.newEdge(edge.getLabel(), toNodeId, false).modify();
            for (final String propertyKey : edge.keySet())
                if (!Edge.IGNORED_FIELDS.contains(propertyKey)) {
                    Object value = edge.getProperty(propertyKey);
                    if (value instanceof Collection)
                        value = convertCollectionToArray((Collection<?>) value);
                    if (value != null)
                        arcadeEdge.set(propertyKey, value);
                }
        }
    }

    private void createIndices(final DatabaseInternal db, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Creating indices...");
        final IndexDescription[] indices = graph.indexDescriptions();
        for (final IndexDescription index : indices) {
            if (LOGGER.isInfoEnabled())
                LOGGER.info("Creating " + index.getType() + " index on '" + index.getProperty() + "' field for " +
                            index.getTarget() + " label '" + index.getLabel() + "'...");
            final boolean isUnique = index.getType() == IndexDescription.Type.UNIQUE;
            db.getSchema().createTypeIndex(Schema.INDEX_TYPE.LSM_TREE, isUnique, index.getLabel(),
                                           new String[]{index.getProperty()}, LSMTreeIndexAbstract.DEF_PAGE_SIZE,
                                           LSMTreeIndexAbstract.NULL_STRATEGY.SKIP, null);
        }
    }

    public void openBrowser() {
        final int port = 2480; // TODO
        try {
            Desktop.getDesktop().browse(new URI("http://localhost:" + port + "/"));
        } catch (IOException | URISyntaxException e) {
            if (LOGGER.isErrorEnabled())
                LOGGER.error("Failed to open Browser", e);
        }
    }
}
