package com.gayatri.dentalclinic.repository;

import com.gayatri.dentalclinic.entity.PublicRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicRequestRepository extends JpaRepository<PublicRequest, Long> {
}
