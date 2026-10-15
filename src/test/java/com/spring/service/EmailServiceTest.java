package com.spring.service;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EmailService.
 * - JavaMailSender is a MOCK: no SMTP connection, no real mail is ever sent.
 * - @InjectMocks builds EmailService and puts the mock into its
 *   "@Autowired private JavaMailSender mailSender" field.
 * Paths in sendEmail(): try-block succeeds -> true | any exception -> caught -> false
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks                       // creates EmailService and injects the @Mock above
    private EmailService emailService;

    /** A real, empty MimeMessage (no mail server needed to create one). */
    private MimeMessage newMessage() {
        return new MimeMessage((Session) null);
    }

    @Test
    @DisplayName("TC-E01 success: returns true and hands the message to send()")
    void sendEmail_success_returnsTrue() {
        MimeMessage message = newMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);

        boolean result = emailService.sendEmail("Welcome", "<h2>Hi</h2>", "deeps@gmail.com");

        assertThat(result).isTrue();
        verify(mailSender).send(message);          // the SAME message object was sent
    }

    @Test
    @DisplayName("TC-E02 the sent message has the right subject, recipient and HTML body")
    void sendEmail_success_messageIsFilledCorrectly() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(newMessage());
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);

        emailService.sendEmail("Deeps IT - Your Account Created Successfully",
                "<h2> Your Temporary Pwd :X7K2P</h2>", "deeps@gmail.com");

        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        assertThat(sent.getSubject()).isEqualTo("Deeps IT - Your Account Created Successfully");
        assertThat(sent.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("deeps@gmail.com");
        assertThat(sent.getContent().toString()).contains("X7K2P");
    }

    @Test
    @DisplayName("TC-E03 SMTP failure: send() throws -> exception is caught -> returns false")
    void sendEmail_sendFails_returnsFalse() {
        MimeMessage message = newMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);
        // send() returns void, so the "when(...)" style can't be used -> doThrow(...).when(mock).method()
        doThrow(new MailSendException("535 Username and Password not accepted"))
                .when(mailSender).send(message);

        boolean result = emailService.sendEmail("Welcome", "<h2>Hi</h2>", "deeps@gmail.com");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("TC-E04 message can't even be created: returns false and send() is never called")
    void sendEmail_createFails_returnsFalse_andNothingSent() {
        when(mailSender.createMimeMessage()).thenThrow(new IllegalStateException("mail not configured"));

        boolean result = emailService.sendEmail("Welcome", "<h2>Hi</h2>", "deeps@gmail.com");

        assertThat(result).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
