package com.hazelcast.simulator.protocol.connector;

import com.hazelcast.simulator.protocol.configuration.AgentClientConfiguration;
import com.hazelcast.simulator.protocol.configuration.AgentServerConfiguration;
import com.hazelcast.simulator.protocol.configuration.ClientConfiguration;
import com.hazelcast.simulator.protocol.core.Response;
import com.hazelcast.simulator.protocol.core.SimulatorAddress;
import com.hazelcast.simulator.protocol.core.SimulatorMessage;
import com.hazelcast.simulator.protocol.processors.AgentOperationProcessor;

import static com.hazelcast.simulator.protocol.core.AddressLevel.AGENT;

/**
 * Connector which listens for incoming Simulator Coordinator connections and connects to remote Simulator Worker instances.
 */
public class AgentConnector {

    private final SimulatorAddress localAddress;
    private final AgentServerConfiguration configuration;
    private final ServerConnector server;

    /**
     * Creates an {@link AgentConnector}.
     *
     * @param addressIndex the index of this Simulator Agent
     * @param port         the port for incoming connections
     */
    public AgentConnector(int addressIndex, int port) {
        AgentOperationProcessor processor = new AgentOperationProcessor();

        this.localAddress = new SimulatorAddress(AGENT, addressIndex, 0, 0);
        this.configuration = new AgentServerConfiguration(processor, localAddress, port);
        this.server = new ServerConnector(configuration);
    }

    /**
     * Starts to listen on the incoming port.
     */
    public void start() {
        server.start();
    }

    /**
     * Stops to listen on the incoming port.
     */
    public void shutdown() {
        server.shutdown();
    }

    /**
     * Adds a Simulator Worker and connects to it.
     *
     * @param workerIndex the index of the Simulator Worker
     * @param workerHost  the host of the Simulator Worker
     * @param workerPort  the port of the Simulator Worker
     */
    public void addWorker(int workerIndex, String workerHost, int workerPort) {
        // TODO: spawn Simulator Worker instance

        ClientConfiguration clientConfiguration = new AgentClientConfiguration(localAddress, workerIndex, workerHost, workerPort);
        ClientConnector client = new ClientConnector(clientConfiguration);
        client.start();

        configuration.addWorker(workerIndex, client);
    }

    /**
     * Removes a Simulator Worker.
     *
     * @param workerIndex the index of the remote Simulator Worker
     */
    public void removeWorker(int workerIndex) {
        configuration.removeWorker(workerIndex);
    }

    public SimulatorAddress getAddress() {
        return configuration.getLocalAddress();
    }

    public Response write(SimulatorMessage message) throws Exception {
        return server.write(message);
    }
}
