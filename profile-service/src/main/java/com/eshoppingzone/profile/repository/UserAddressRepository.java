package com.eshoppingzone.profile.repository;

import com.eshoppingzone.profile.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    List<UserAddress> findByProfileId(Long profileId);
    Optional<UserAddress> findByAddressIdAndProfileId(Long addressId, Long profileId);
    Optional<UserAddress> findByProfileIdAndIsDefaultTrue(Long profileId);
}
