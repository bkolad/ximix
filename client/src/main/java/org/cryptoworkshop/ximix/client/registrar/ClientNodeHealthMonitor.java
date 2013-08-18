package org.cryptoworkshop.ximix.client.registrar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cryptoworkshop.ximix.client.MonitorService;
import org.cryptoworkshop.ximix.client.NodeDetail;
import org.cryptoworkshop.ximix.common.asn1.message.CommandMessage;
import org.cryptoworkshop.ximix.common.asn1.message.MessageReply;
import org.cryptoworkshop.ximix.common.asn1.message.NodeStatusMessage;
import org.cryptoworkshop.ximix.common.asn1.message.NodeStatusRequestMessage;
import org.cryptoworkshop.ximix.common.service.AdminServicesConnection;
import org.cryptoworkshop.ximix.common.service.ServiceConnectionException;

/**
 *
 */
class ClientNodeHealthMonitor
    implements MonitorService
{
    private AdminServicesConnection connection = null;
    private Map<String, NodeDetail> configuredNodeDetails;

    public ClientNodeHealthMonitor(AdminServicesConnection connection, Map<String, NodeDetail> configuredNodeDetails)
    {
        this.connection = connection;
        this.configuredNodeDetails = Collections.unmodifiableMap(configuredNodeDetails);
    }

    @Override
    public Map<String, NodeDetail> getConfiguredNodeDetails()
    {
        return configuredNodeDetails;
    }

    @Override
    public NodeStatusMessage getStatistics(String name)
        throws ServiceConnectionException
    {
        NodeStatusMessage out = null;

        MessageReply reply = connection.sendMessage(name, CommandMessage.Type.NODE_STATISTICS, NodeStatusRequestMessage.forStatisticsRequest());
        if (reply.getType() == MessageReply.Type.ERROR)
        {
            System.out.println("Got error requesting statistics.");
        }
        else
        {
            out = NodeStatusMessage.getInstance(reply.getPayload());
        }


        return out;
    }

    @Override
    public List<NodeStatusMessage> getFullInfo()
        throws ServiceConnectionException
    {
        List<NodeStatusMessage> out = new ArrayList<>();

        for (String name : connection.getActiveNodeNames())
        {
            MessageReply reply = connection.sendMessage(name, CommandMessage.Type.NODE_STATISTICS, NodeStatusRequestMessage.forFullDetails());
            if (reply.getType() == MessageReply.Type.ERROR)
            {
                System.out.println("Got error requesting vm info.");
            }
            else
            {
                out.add(NodeStatusMessage.getInstance(reply.getPayload()));
            }
        }

        return out;
    }

    @Override
    public Set<String> getConnectedNodeNames()
    {
        return connection.getActiveNodeNames();
    }

    @Override
    public NodeStatusMessage getFullInfo(String name)
        throws ServiceConnectionException
    {
        MessageReply reply = connection.sendMessage(name, CommandMessage.Type.NODE_STATISTICS, NodeStatusRequestMessage.forFullDetails());

        if (reply.getType() == MessageReply.Type.ERROR)
        {
            return null;
        }

        return NodeStatusMessage.getInstance(reply.getPayload());
    }


}