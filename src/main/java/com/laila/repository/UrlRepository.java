package com.laila.repository;

import com.laila.entities.Url;
import org.springframework.data.repository.CrudRepository;

public interface UrlRepository extends CrudRepository<Url, Long> {
    Iterable<Url> findByUserId(String userId);

}
