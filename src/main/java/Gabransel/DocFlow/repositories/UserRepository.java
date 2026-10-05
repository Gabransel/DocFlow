package Gabransel.DocFlow.repositories;

import Gabransel.DocFlow.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
    Optional<User> findByEmail(String email);
    Page<User> findAllByActiveTrue(Pageable pageable);
}

