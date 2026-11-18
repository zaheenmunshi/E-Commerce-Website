package com.zaheenmunshi.ecommerce.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

import com.zaheenmunshi.ecommerce.config.AppProperties;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Emails the customer once the order is committed. Runs async so a slow or broken
 * mail server never fails or delays checkout. Off unless {@code app.mail.enabled=true}.
 */
@Component
public class OrderConfirmationMailer {

	private static final Logger log = LoggerFactory.getLogger(OrderConfirmationMailer.class);

	private final JavaMailSender mailSender;

	private final AppProperties.Mail settings;

	public OrderConfirmationMailer(JavaMailSender mailSender, AppProperties properties) {
		this.mailSender = mailSender;
		this.settings = properties.mail();
	}

	@Async
	@TransactionalEventListener
	public void onOrderPlaced(OrderPlacedEvent event) {
		if (!settings.enabled()) {
			log.info("Order {} placed; confirmation mail skipped (app.mail.enabled=false)", event.orderId());
			return;
		}
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
			helper.setFrom(settings.from());
			helper.setTo(event.customerEmail());
			helper.setSubject("Clothing : Order Placed");
			helper.setText(body(event), true);
			mailSender.send(message);
		}
		catch (MailException | MessagingException ex) {
			log.warn("Could not send confirmation for order {}", event.orderId(), ex);
		}
	}

	private static String body(OrderPlacedEvent event) {
		return """
				<div style="font-family:sans-serif;background:#f5f5f5;padding:40px;text-align:center">
				  <h1 style="text-transform:capitalize">dear %s, <span style="font-weight:300">your order is successfully placed</span></h1>
				  <p>Order <b>%s</b> &middot; total <b>$%s</b></p>
				</div>
				""".formatted(HtmlUtils.htmlEscape(event.customerName()), event.orderId(), event.totalAmount());
	}

}
