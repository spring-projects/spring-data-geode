/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.gemfire.test.support;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import org.awaitility.Awaitility;
import org.junit.rules.TemporaryFolder;
import org.springframework.data.gemfire.process.ProcessExecutor;
import org.springframework.data.gemfire.process.ProcessWrapper;

import org.apache.geode.distributed.LocatorLauncher;
import org.apache.geode.distributed.ServerLauncher;
import org.apache.geode.management.LocatorMXBean;
import org.apache.geode.management.MemberMXBean;

/**
 * Utility class to launch and manage a static Geode cluster as a unit within tests. For example:
 * <pre>
 *   private static ClusterStarter cluster = new ClusterStarter();
 *  {@literal @}BeforeClass
 *   public static void setup() throws Exception {
 *     cluster.withLocator("locator")
 *         .withLogging()
 *         .withDebugging(5005, false)
 *         .withArgs("-port", "0");
 *     cluster.withServer("server-1")
 *         .withLogging()
 *         .withLocatorPort();
 *
 *     cluster.launch();
 *   }
 *
 *  {@literal @}AfterClass
 *   public static void teardown() throws Exception {
 *     cluster.shutdown();
 *   }
 * </pre>
 *
 * The cluster assumes an optional, single locator and any number of servers. If launched, the
 * locator's port can be retrieved with {@link #getLocatorPort()}. This allows for the locator to
 * be launched with an ephemeral port.
 *
 * @author Jens Deppe
 */
public class ClusterStarter {

  private Map<String, ProcessWrapper> trackedProcesses = new LinkedHashMap<>();
  private Map<String, MemberBuilder> memberBuilders = new LinkedHashMap<>();
  private TemporaryFolder tmpDir = new TemporaryFolder();
  private int locatorPort;

  public void launch() throws Exception {
    tmpDir.create();
    for (Map.Entry<String, MemberBuilder> builder : memberBuilders.entrySet()) {
      MemberBuilder b = builder.getValue();
      File memberWorkingDir = tmpDir.newFolder(builder.getKey());

      for (BiConsumer<MemberBuilder, ClusterStarter> c : b.getDeferredConsumers()) {
        c.accept(b, this);
      }

      ProcessWrapper process = ProcessExecutor
          .launch(builder.getKey(), memberWorkingDir, b.getClasspath(), b.getMainClass(),
              b.getLogConsumer(), b.getProcessStartupWaiter(), b.args.toArray(new String[]{}));

      trackedProcesses.put(builder.getKey(), process);
    }
  }

  public void shutdown() {
    List<ProcessWrapper> reversed = new ArrayList<>(trackedProcesses.values());
    Collections.reverse(reversed);

    for (ProcessWrapper process : reversed) {
      process.stop();
    }

    tmpDir.delete();
  }

  public int getLocatorPort() {
    return locatorPort;
  }

  public MemberBuilder withLocator(String name) {
    MemberBuilder builder = new MemberBuilder(name, LocatorLauncher.class);
    builder.withArgs("start", name)
        .withArgs("-Dgemfire.jmx-manager-start=true");
    builder.withProcessStartupWaiter(getLocatorStartupWaiter(name));
    memberBuilders.put(name, builder);

    return builder;
  }

  public MemberBuilder withServer(String name) {
    MemberBuilder builder = new MemberBuilder(name, ServerLauncher.class);
    builder.withArgs("start", name)
        .withArgs("-Dgemfire.http-service-port=0");
    builder.withProcessStartupWaiter(getServerStartupWaiter(name));
    memberBuilders.put(name, builder);

    return builder;
  }

  private Consumer<ProcessWrapper> getLocatorStartupWaiter(String name) {
    return (wrapper) -> {
      try {
        int pid = getMemberPid(wrapper);
        MBeanServerConnection connection = getMBeanServerConnection(pid);
        waitForMemberToBeOnline(name, connection);

        ObjectName locatorObjectName = new ObjectName(String.format("GemFire:service=Locator,type=Member,member=%s", name));
        LocatorMXBean member =
            JMX.newMXBeanProxy(connection, locatorObjectName, LocatorMXBean.class);
        locatorPort = member.getPort();
      } catch (IOException | AttachNotSupportedException | MalformedObjectNameException ex) {
        throw new RuntimeException(ex);
      }
    };

  }

  private Consumer<ProcessWrapper> getServerStartupWaiter(String name) {
    return (wrapper) -> {
      try {
        int pid = getMemberPid(wrapper);
        MBeanServerConnection connection = getMBeanServerConnection(pid);
        waitForMemberToBeOnline(name, connection);
      } catch (IOException | AttachNotSupportedException | MalformedObjectNameException ex) {
        throw new RuntimeException(ex);
      }
    };
  }

  private void waitForMemberToBeOnline(String name, MBeanServerConnection connection) throws
      MalformedObjectNameException  {
    ObjectName memberObjectName =
        new ObjectName(String.format("GemFire:type=Member,member=%s", name));
    MemberMXBean member =
        JMX.newMXBeanProxy(connection, memberObjectName, MemberMXBean.class);

    // Wait until the bean is available
    Awaitility.await().atMost(30, TimeUnit.SECONDS).ignoreExceptions()
        .until(() -> member.status() != null);

    // Wait until the status is 'online'
    Awaitility.await().ignoreExceptions().atMost(30, TimeUnit.SECONDS).ignoreExceptions()
        .until(() -> member.status().contains("\"status\":\"online\""));
  }

  private int getMemberPid(ProcessWrapper wrapper) {
    Awaitility.await().atMost(5, TimeUnit.SECONDS).ignoreExceptions().untilAsserted(wrapper::getPid);
    return wrapper.getPid();
  }

  private MBeanServerConnection getMBeanServerConnection(int pid) throws IOException, AttachNotSupportedException {
    List<VirtualMachineDescriptor> vms = VirtualMachine.list();
    for (VirtualMachineDescriptor desc : vms) {
      VirtualMachine vm = VirtualMachine.attach(desc);

      if (!vm.id().equals(Integer.toString(pid))) {
        continue;
      }

      Properties props = vm.getAgentProperties();
      String connectorAddress =
          props.getProperty("com.sun.management.jmxremote.localConnectorAddress");

      if (connectorAddress == null) {
        throw new RuntimeException("com.sun.management.jmxremote.localConnectorAddress property not available. Process must be started with -Dcom.sun.management.jmxremote");
      }

      JMXServiceURL url = new JMXServiceURL(connectorAddress);
      JMXConnector connector = JMXConnectorFactory.connect(url);

      return connector.getMBeanServerConnection();
    }

    throw new RuntimeException("Unable to create JMX connection to pid " + pid);
  }

  public static class MemberBuilder {
    private List<String> args = new ArrayList<>();
    private String marker;
    private Class<?> mainClass;
    private List<String> javaClasspath;
    private Consumer<String> logConsumer;
    private Consumer<ProcessWrapper> processStartupWaiter;
    private List<BiConsumer<MemberBuilder, ClusterStarter>> deferredConsumers = new ArrayList<>();

    public MemberBuilder(String marker, Class<?> mainClass) {
      this.marker = marker;
      this.mainClass = mainClass;
      this.logConsumer = x -> {};
      this.processStartupWaiter = x -> {};

      javaClasspath = Arrays.stream(System.getProperty("java.class.path")
          .split(File.pathSeparator)).collect( Collectors.toList());
      args.add("-Dcom.sun.management.jmxremote");
    }

    public MemberBuilder reduceClasspathTo(String... includes) {
      List<String> includesList = Arrays.stream(includes).collect(Collectors.toList());
      javaClasspath = javaClasspath.stream()
              .filter(x -> includesList.stream().anyMatch(e -> x.contains(e)))
              .collect(Collectors.toList());

      return this;
    }

    public MemberBuilder reduceClasspathBy(String... excludes) {
      List<String> excludesList = Arrays.stream(excludes).collect(Collectors.toList());
      javaClasspath = javaClasspath.stream()
              .filter(x -> excludesList.stream().noneMatch(e -> x.contains(e)))
              .collect(Collectors.toList());

      return this;
    }

    private Class<?> getMainClass() {
      return mainClass;
    }

    private String getClasspath() {
      return String.join(File.pathSeparator, javaClasspath);
    }

    private Consumer<String> getLogConsumer() {
      return logConsumer;
    }

    private List<BiConsumer<MemberBuilder, ClusterStarter>> getDeferredConsumers() {
      return deferredConsumers;
    }

    public MemberBuilder withLocatorPort() {
      return withDeferredConsumer((x, y) -> {
        x.withArgs(String.format("-Dgemfire.locators=localhost[%d]", y.getLocatorPort()));
      });
    }

    private MemberBuilder withDeferredConsumer(BiConsumer<MemberBuilder, ClusterStarter> consumer) {
      deferredConsumers.add(consumer);
      return this;
    }

    public MemberBuilder withLogging() {
      withArgs("-Dlogback.log.level=INFO");
      logConsumer = input -> System.out.printf("[%s] - %s%n", marker, input);
      return this;
    }

    public MemberBuilder withDebugging(int port, boolean suspended) {
      args.add(String.format("-agentlib:jdwp=transport=dt_socket,server=y,suspend=%s,address=%d",
          (suspended ? "y" : "n"), port));
      return this;
    }

    public MemberBuilder withProcessStartupWaiter(Consumer<ProcessWrapper> waiter) {
      processStartupWaiter = waiter;
      return this;
    }

    private Consumer<ProcessWrapper> getProcessStartupWaiter() {
      return processStartupWaiter;
    }

    public MemberBuilder withArgs(String... args) {
      this.args.addAll(Arrays.stream(args).collect(Collectors.toList()));
      return this;
    }

    private List<String> getArgs() {
      return args;
    }
  }

}
