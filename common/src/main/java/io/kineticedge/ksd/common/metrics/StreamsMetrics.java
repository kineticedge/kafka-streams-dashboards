package io.kineticedge.ksd.common.metrics;

import org.apache.kafka.streams.TopologyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * Provide a meaningful name for the tasks identifiers.
 *
 */
public final class StreamsMetrics {

  private static final Logger log = LoggerFactory.getLogger(StreamsMetrics.class);

  private final static ObjectName objectName;

  static {
    try {
      objectName = new ObjectName("application:type=streams");
    } catch (final MalformedObjectNameException e) {
      throw new RuntimeException(e);
    }
  }

  public static void register(final TopologyDescription topologyDescription) {
    try {
      final StreamApplication mBean = new StreamApplication(topologyDescription);
      ManagementFactory.getPlatformMBeanServer().registerMBean(mBean, objectName);
      Runtime.getRuntime().addShutdownHook(new Thread(StreamsMetrics::unregister));
    } catch (final JMException e) {
      log.error("unable to create mbean", e);
    }
  }

  public static void unregister() {
    try {
      MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      if (server.isRegistered(objectName)) {
        server.unregisterMBean(objectName);
      }
    } catch (final JMException e) {
      log.warn("unable to unregister mbean", e);
    }
  }

  public static class StreamApplication implements DynamicMBean {

    private final MBeanInfo mbeanInfo;

    private final Map<String, String> subtopologies;

    public StreamApplication(final TopologyDescription topologyDescription) {
      mbeanInfo = mbeanInfo(topologyDescription);

      subtopologies = topologyDescription.subtopologies().stream()
              .collect(Collectors.toMap(subtopology -> Integer.toString(subtopology.id()), StreamApplication::description));
    }

    @Override
    public Object getAttribute(String attribute)
            throws AttributeNotFoundException, MBeanException, ReflectionException {
      return subtopologies.get(attribute);
    }

    @Override
    public MBeanInfo getMBeanInfo() {
      return mbeanInfo;
    }

    private static String description(final TopologyDescription.Subtopology subtopology) {

      final StringBuilder builder = new StringBuilder();

      builder.append(subtopology.nodes().stream()
              .filter(node -> node instanceof TopologyDescription.Source)
              .map(TopologyDescription.Node::name)
              .collect(Collectors.joining(",")));
      builder.append("__");
      builder.append(subtopology.nodes().stream()
              .filter(node -> node instanceof TopologyDescription.Sink)
              .map(TopologyDescription.Node::name)
              .collect(Collectors.joining(",")));

      return builder.toString();
    }

    private static MBeanInfo mbeanInfo(final TopologyDescription topologyDescription) {
      return new MBeanInfo(StreamsMetrics.class.getName(),
              null,
              mBeanAttributeInfos(topologyDescription),
              null,
              null,
              new MBeanNotificationInfo[0]);
    }

    private static MBeanAttributeInfo[] mBeanAttributeInfos(final TopologyDescription topologyDescription) {
      return topologyDescription.subtopologies().stream()
              .map(subtopology -> new MBeanAttributeInfo("" + subtopology.id(), Integer.class.getSimpleName(), "task description", true, false, false))
              .toArray(MBeanAttributeInfo[]::new);
    }


    @Override
    public AttributeList getAttributes(String[] names) {
      AttributeList list = new AttributeList();
      for (String name : names) {
        try {
          list.add(new Attribute(name, getAttribute(name)));
        } catch (Exception e) {
          log.warn("Error getting JMX attribute '{}'", name, e);
        }
      }
      return list;
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
      throw new UnsupportedOperationException("Set not allowed.");
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
      throw new UnsupportedOperationException("Set not allowed.");
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) {
      throw new UnsupportedOperationException("invoke not allowed.");
    }


  }

}
