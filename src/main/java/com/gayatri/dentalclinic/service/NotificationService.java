package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.entity.Appointment;
import com.gayatri.dentalclinic.entity.Dentist;
import com.gayatri.dentalclinic.entity.Patient;

public interface NotificationService {

    void sendAppointmentConfirmation(Patient patient, Dentist dentist, Appointment appointment);
}
