package com.hortonworks.streaming.impl.topologies;

import javax.jms.Session;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.contrib.jms.JmsProvider;
import backtype.storm.contrib.jms.JmsTupleProducer;
import backtype.storm.contrib.jms.spout.JmsSpout;
import backtype.storm.topology.TopologyBuilder;

import com.hortonworks.streaming.impl.bolts.TruckEventRuleBolt;
import com.hortonworks.streaming.spouts.JsonTupleProducer;
import com.hortonworks.streaming.spouts.SpringJmsProvider;

public class TruckEventProcessorTopology extends BaseTruckEventTopology{
	public static final String JMS_QUEUE_SPOUT = "sensor_data_spout";
	public static final String TRUCK_EVENT_RULE_BOLT = "truck_event_rule_bolt";

	
	public  TruckEventProcessorTopology(String configFileLocation) throws Exception{
		super(configFileLocation);
	}
	
	
	private void buildAndSubmit() throws Exception {
		JmsProvider jmsQueueProvider = new SpringJmsProvider(
				"jms-activemq.xml", "jmsConnectionFactory", "notificationQueue");
		// JMS Producer
		JmsTupleProducer producer = new JsonTupleProducer();
		// JMS Queue Spout
		JmsSpout queueSpout = new JmsSpout();
		queueSpout.setJmsProvider(jmsQueueProvider);
		queueSpout.setJmsTupleProducer(producer);
		queueSpout.setJmsAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
		queueSpout.setDistributed(true); // allow multiple instances

		TopologyBuilder builder = new TopologyBuilder();

		// spout with 5 parallel instances
		builder.setSpout(JMS_QUEUE_SPOUT, queueSpout, 5);
		builder.setBolt(TRUCK_EVENT_RULE_BOLT, new TruckEventRuleBolt(this.topologyConfig))
				.shuffleGrouping(JMS_QUEUE_SPOUT);

		Config conf = new Config();
		conf.setDebug(false);

		LocalCluster cluster = new LocalCluster();
		cluster.submitTopology("truck-event-processor", conf,
				builder.createTopology());		
		
	}
	
	public static void main(String[] args) throws Exception {
		String configFileLocation = args[0];
		TruckEventProcessorTopology topology = new TruckEventProcessorTopology(configFileLocation);
		topology.buildAndSubmit();
	}
	
}
