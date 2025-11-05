package com.innowise.userservice.repository.specification;

import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.BussinessException;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    private UserSpecification() {
        throw new UnsupportedOperationException("Cannot create an object of utility class");
    }

    public static Specification<User> hasName(String name) {
        return ((root, query, criteriaBuilder) -> {
           if (name == null || name.isBlank()) {
               return criteriaBuilder.conjunction();
           }
           return criteriaBuilder.like(criteriaBuilder.lower(root.get("name")),
                   "%" + name.toLowerCase() + "%");
        });
    }

    public static Specification<User> hasSurname(String surname) {
        return ((root, query, criteriaBuilder) -> {
            if (surname == null || surname.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("surname")),
                    "%" + surname.toLowerCase() + "%");
        });
    }

    public static Specification<User> isActive(Boolean active) {
        return ((root, query, criteriaBuilder) -> {
            if (active == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("active"), active);
        });
    }
}
