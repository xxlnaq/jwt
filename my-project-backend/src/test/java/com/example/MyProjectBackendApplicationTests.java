package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class MyProjectBackendApplicationTests {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	private final String testQueueName = "testQueue";

	@Bean
	public Queue testQueue() {
		return new Queue(testQueueName, true);
	}

	@Test
	public void testRabbitMQConnection() {
		String testMessage = "Hello, RabbitMQ!";
		rabbitTemplate.convertAndSend(testQueueName, testMessage);

		String receivedMessage = (String) rabbitTemplate.receiveAndConvert(testQueueName);
		assertEquals(testMessage, receivedMessage, "The received message should match the sent message");
	}

	@RabbitListener(queues = "testQueue")
	public void receiveMessage(String message) {
		System.out.println("Received message: " + message);
	}
}