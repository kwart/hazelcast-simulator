package com.hazelcast.simulator.protocol;

import com.hazelcast.simulator.protocol.connector.AgentConnector;
import com.hazelcast.simulator.protocol.connector.CoordinatorConnector;
import com.hazelcast.simulator.protocol.connector.WorkerConnector;
import com.hazelcast.simulator.protocol.core.AddressLevel;
import com.hazelcast.simulator.protocol.core.Response;
import com.hazelcast.simulator.protocol.core.ResponseType;
import com.hazelcast.simulator.protocol.core.SimulatorAddress;
import com.hazelcast.simulator.protocol.core.SimulatorMessage;
import com.hazelcast.simulator.protocol.processors.OperationProcessor;
import com.hazelcast.simulator.protocol.processors.TestOperationProcessor;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.hazelcast.simulator.protocol.core.SimulatorAddress.COORDINATOR;
import static org.junit.Assert.assertEquals;

public class ProtocolUtil {

    private static final int MAX_ADDRESS_INDEX = 3;
    private static final AddressLevel MIN_ADDRESS_LEVEL = AddressLevel.AGENT;

    private static final String MESSAGE_DATA = "{\"testId\":\"StringStringMapTest\"}";
    private static final int MIN_ADDRESS_LEVEL_VALUE = MIN_ADDRESS_LEVEL.toInt();

    private static final Random RANDOM = new Random();
    private static final AtomicLong MESSAGE_ID = new AtomicLong();

    private static final Logger ROOT_LOGGER = Logger.getRootLogger();
    private static final AtomicReference<Level> LOGGER_LEVEL = new AtomicReference<Level>();

    static void setLogLevel(Level level) {
        if (LOGGER_LEVEL.compareAndSet(null, ROOT_LOGGER.getLevel())) {
            ROOT_LOGGER.setLevel(level);
        }
    }

    static void resetLogLevel() {
        Level level = LOGGER_LEVEL.get();
        if (level != null && LOGGER_LEVEL.compareAndSet(level, null)) {
            ROOT_LOGGER.setLevel(level);
        }
    }

    static WorkerConnector startWorker(int addressIndex, int parentAddressIndex, int port) {
        return startWorker(addressIndex, parentAddressIndex, port, 2);
    }

    static WorkerConnector startWorker(int addressIndex, int parentAddressIndex, int port, int numberOfTests) {
        WorkerConnector workerConnector = new WorkerConnector(addressIndex, parentAddressIndex, port);

        OperationProcessor processor = new TestOperationProcessor();
        for (int testIndex = 1; testIndex <= numberOfTests; testIndex++) {
            workerConnector.addTest(testIndex, processor);
        }

        workerConnector.start();
        return workerConnector;
    }

    static AgentConnector startAgent(int addressIndex, int port, String workerHost, int workerStartPort) {
        return startAgent(addressIndex, port, workerHost, workerStartPort, 2);
    }

    static AgentConnector startAgent(int addressIndex, int port, String workerHost, int workerStartPort, int numberOfWorkers) {
        AgentConnector agentConnector = new AgentConnector(addressIndex, port);
        for (int workerIndex = 1; workerIndex <= numberOfWorkers; workerIndex++) {
            agentConnector.addWorker(workerIndex, workerHost, workerStartPort + workerIndex);
        }

        agentConnector.start();
        return agentConnector;
    }

    static CoordinatorConnector startCoordinator(String agentHost, int agentStartPort) {
        return startCoordinator(agentHost, agentStartPort, 2);
    }

    static CoordinatorConnector startCoordinator(String agentHost, int agentStartPort, int numberOfAgents) {
        CoordinatorConnector coordinatorConnector = new CoordinatorConnector();
        for (int i = 1; i <= numberOfAgents; i++) {
            coordinatorConnector.addAgent(i, agentHost, agentStartPort + i);
        }

        return coordinatorConnector;
    }

    static void resetMessageId() {
        MESSAGE_ID.set(0);
    }

    static SimulatorMessage buildRandomMessage() {
        int addressLevelValue = MIN_ADDRESS_LEVEL_VALUE + RANDOM.nextInt(AddressLevel.values().length - MIN_ADDRESS_LEVEL_VALUE);
        AddressLevel addressLevel = AddressLevel.fromInt(addressLevelValue);

        int agentIndex = RANDOM.nextInt(MAX_ADDRESS_INDEX + 1);
        int workerIndex = RANDOM.nextInt(MAX_ADDRESS_INDEX + 1);
        int testIndex = RANDOM.nextInt(MAX_ADDRESS_INDEX + 1);

        switch (addressLevel) {
            case COORDINATOR:
                return buildMessage(COORDINATOR);
            case AGENT:
                return buildMessage(new SimulatorAddress(addressLevel, agentIndex, 0, 0));
            case WORKER:
                return buildMessage(new SimulatorAddress(addressLevel, agentIndex, workerIndex, 0));
            case TEST:
                return buildMessage(new SimulatorAddress(addressLevel, agentIndex, workerIndex, testIndex));
            default:
                throw new IllegalArgumentException("Unsupported addressLevel: " + addressLevel);
        }
    }

    static SimulatorMessage buildMessage(SimulatorAddress destination) {
        return buildMessage(destination, COORDINATOR);
    }

    static SimulatorMessage buildMessage(SimulatorAddress destination, SimulatorAddress source) {
        return new SimulatorMessage(destination, source, MESSAGE_ID.incrementAndGet(), 0, MESSAGE_DATA);
    }

    static void assertSingleTarget(Response response, SimulatorAddress destination, ResponseType responseType) {
        assertAllTargets(response, destination, responseType, 1);
    }

    static void assertAllTargets(Response response, SimulatorAddress destination, ResponseType responseType, int responseCount) {
        assertEquals(responseCount, response.entrySet().size());
        for (Map.Entry<SimulatorAddress, ResponseType> entry : response.entrySet()) {
            assertEquals(responseType, entry.getValue());
            assertEquals(destination.getAddressLevel(), entry.getKey().getAddressLevel());
        }
    }
}
