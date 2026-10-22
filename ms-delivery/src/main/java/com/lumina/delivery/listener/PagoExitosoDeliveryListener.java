package com.lumina.delivery.listener;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


import java.io.IOException;

@Component
public class PagoExitosoDeliveryListener {

    private static final Logger log = LoggerFactory.getLogger(PagoExitosoDeliveryListener.class);
    public static final String QUEUE_DELIVERY = "lumina.delivery.queue";

    @RabbitListener(queues = QUEUE_DELIVERY)
    public void procesarOrdenDelivery(Object payload, Channel channel, Message message) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            log.info("MS-DELIVERY: Recibido evento de pago -> {}", payload);

            // Confirmación manual explícita (ACK)
            channel.basicAck(deliveryTag, false);
            log.info("MS-DELIVERY: Orden de despacho generada con éxito (ACK).");

        } catch (Exception e) {
            log.error("MS-DELIVERY: Error al generar despacho. Redirigiendo a DLQ...", e);
            // requeue = false envía el mensaje fallido a la Dead Letter Queue (DLQ)
            channel.basicNack(deliveryTag, false, false);
        }
    }
}