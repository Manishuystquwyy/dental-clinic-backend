package com.gayatri.dentalclinic.repository;

import com.gayatri.dentalclinic.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillRepository extends JpaRepository<Bill, Long> {
}
