package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.config.AppointmentMailConfig;
import com.gayatri.dentalclinic.entity.Appointment;
import com.gayatri.dentalclinic.entity.Dentist;
import com.gayatri.dentalclinic.entity.Patient;
import java.sql.Connection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppointmentConfirmationTransactionTest {
    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final Connection connection = mock(Connection.class);
    private final Patient patient = new Patient();
    private final Dentist dentist = new Dentist();
    private final Appointment appointment = new Appointment();
    private final CountDownLatch releaseMail = new CountDownLatch(1);
    private final AtomicBoolean committed = new AtomicBoolean();
    private ThreadPoolTaskExecutor executor;
    private NotificationServiceImpl service;
    private TransactionTemplate transaction;

    @BeforeEach
    void setUp() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        doAnswer(invocation -> { committed.set(true); return null; }).when(connection).commit();
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        executor = new AppointmentMailConfig().appointmentMailExecutor(1, 1);
        executor.initialize();
        service = new NotificationServiceImpl(sender, executor, "clinic@example.com", "", "");
        patient.setEmail("patient@example.com");
        patient.setFirstName("Original");
        patient.setLastName("Patient");
        dentist.setName("Dentist");
        appointment.setId(42L);
    }

    @AfterEach
    void tearDown() {
        releaseMail.countDown();
        executor.shutdown();
    }

    @Test
    void slowSmtpStartsOnlyAfterCommitWithoutHoldingUpConnectionRelease() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean committedAtSend = new AtomicBoolean();
        AtomicBoolean workerHasTransaction = new AtomicBoolean(true);
        doAnswer(invocation -> {
            committedAtSend.set(committed.get());
            workerHasTransaction.set(TransactionSynchronizationManager.isActualTransactionActive());
            started.countDown();
            releaseMail.await(5, TimeUnit.SECONDS);
            return null;
        }).when(sender).send(any(SimpleMailMessage.class));
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(3), () -> transaction.executeWithoutResult(status -> {
            service.sendAppointmentConfirmation(patient, dentist, appointment);
            verifyNoInteractions(sender);
            patient.setFirstName("Changed");
            patient.setEmail("changed@example.com");
        }));
        assertTrue(started.await(2, TimeUnit.SECONDS));
        assertTrue(committedAtSend.get());
        assertFalse(workerHasTransaction.get());
        verify(connection).close(); // SMTP is still blocked, but DB cleanup completed.
        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertEquals("patient@example.com", message.getValue().getTo()[0]);
        assertTrue(message.getValue().getText().contains("Original Patient"));
    }

    @Test
    void outerRollbackDiscardsConfirmation() {
        transaction.executeWithoutResult(outer -> {
            transaction.executeWithoutResult(inner -> service.sendAppointmentConfirmation(patient, dentist, appointment));
            assertEquals(0, executor.getThreadPoolExecutor().getTaskCount());
            outer.setRollbackOnly();
        });
        assertEquals(0, executor.getThreadPoolExecutor().getTaskCount());
        verifyNoInteractions(sender);
    }

    @Test
    void commitFailureDoesNotSendConfirmation() throws Exception {
        doThrow(new java.sql.SQLException("commit failed")).when(connection).commit();
        assertThrows(org.springframework.transaction.TransactionException.class,
                () -> transaction.executeWithoutResult(status -> service.sendAppointmentConfirmation(patient, dentist, appointment)));
        assertEquals(0, executor.getThreadPoolExecutor().getTaskCount());
        verifyNoInteractions(sender);
    }

    @Test
    void fullQueueDoesNotRunMailOnCallerOrFailCommittedBooking() throws Exception {
        CountDownLatch workerBusy = new CountDownLatch(1);
        executor.execute(() -> {
            workerBusy.countDown();
            try { releaseMail.await(5, TimeUnit.SECONDS); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
        });
        assertTrue(workerBusy.await(2, TimeUnit.SECONDS));
        executor.execute(() -> {}); // Fill the one available queue entry.
        assertDoesNotThrow(() -> transaction.executeWithoutResult(status -> service.sendAppointmentConfirmation(patient, dentist, appointment)));
        assertTrue(committed.get());
        verifyNoInteractions(sender);
    }

    @Test
    void smtpFailureDoesNotFailBookingAndWorkerRemainsUsable() throws Exception {
        doThrow(new MailSendException("SMTP unavailable")).when(sender).send(any(SimpleMailMessage.class));
        assertDoesNotThrow(() -> transaction.executeWithoutResult(status -> service.sendAppointmentConfirmation(patient, dentist, appointment)));
        verify(sender, timeout(2000)).send(any(SimpleMailMessage.class));
        assertTrue(committed.get());
    }

    @Test
    void nonTransactionalCallAlsoUsesWorker() throws Exception {
        service.sendAppointmentConfirmation(patient, dentist, appointment);
        verify(sender, timeout(2000)).send(any(SimpleMailMessage.class));
    }
}
